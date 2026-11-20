//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#include <cmath>

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "model/Scene.h"
#include "model/history/CameraState.h"
#include "render/ProjectedSurfacesViewBuilder.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"

const ColorRgb ProjectedSurfacesViewBuilder::NO_LIGHT_COLOR(0.8, 0.8, 0.8);

ProjectedSurfacesViewBuilder::CachedView::CachedView()
    : viewport(nullptr), cameraState(nullptr), width(0), height(0),
      generation(0), withShadows(false)
{
}

ProjectedSurfacesViewBuilder::CachedView::~CachedView()
{
    delete cameraState;
    for ( long i = 0; i < polygons.size(); i++ ) {
        delete polygons[i];
    }
}

ProjectedSurfacesViewBuilder::~ProjectedSurfacesViewBuilder()
{
    for ( long i = 0; i < cache.size(); i++ ) {
        delete cache[i];
    }
    for ( long i = 0; i < surrogates.size(); i++ ) {
        delete surrogates[i];
    }
}

const java::ArrayList<ProjectedPolygon*>& ProjectedSurfacesViewBuilder::buildView(
    Scene* scene, Viewport* viewport, int width, int height,
    long long generation, bool withShadows)
{
    Camera* camera = viewport->getActiveCamera();
    CameraState* cameraState = CameraState::capture(camera);
    CachedView* cached = nullptr;
    long cachedIndex = -1;

    for ( long i = 0; i < cache.size(); i++ ) {
        if ( cache[i]->viewport == viewport ) {
            cached = cache[i];
            cachedIndex = i;
            break;
        }
    }
    if ( cached != nullptr && cached->width == width &&
         cached->height == height && cached->generation == generation &&
         cached->withShadows == withShadows &&
         cached->cameraState->isSameState(cameraState) ) {
        delete cameraState;
        return cached->polygons;
    }

    java::ArrayList<SimpleBody*> solids;
    java::ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        SimpleBody* body = bodies[i];
        if ( scene->isHidden(body) || body->getGeometry() == nullptr ) {
            continue;
        }
        PolyhedralBoundedSolid* brep = brepCache.get(body->getGeometry());
        if ( brep != nullptr ) {
            solids.add(brepBody(body, brep));
        }
    }

    java::ArrayList<ProjectedPolygon*> polygons;
    java::ArrayList<Light*>& lights = scene->scene->getLights();
    bool shadows = withShadows && !lights.isEmpty();
    if ( !solids.isEmpty() ) {
        java::ArrayList<HiddenSurfaceRenderer::VisiblePolygon*> visible = shadows ?
            HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(solids,
                *camera, lights) :
            HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(solids,
                *camera);
        // The color depends on the face and, with shadows, on the
        // lights reaching the polygon
        java::ArrayList<ColorEntry> colors;
        for ( long i = 0; i < visible.size(); i++ ) {
            HiddenSurfaceRenderer::VisiblePolygon* polygon = visible[i];
            java::ArrayList<const void*> key;
            key.add(polygon->face);
            key.add(polygon->body);
            if ( polygon->lights != nullptr ) {
                for ( long l = 0; l < polygon->lights->size(); l++ ) {
                    key.add((*polygon->lights)[l]);
                }
            }
            ColorRgb color;
            bool found = false;
            for ( long c = 0; c < colors.size(); c++ ) {
                if ( colors[c].key.equals(key) ) {
                    color = colors[c].color;
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                color = shadeFace(polygon, scene, camera,
                    polygon->lights != nullptr ? *polygon->lights : lights);
                ColorEntry entry;
                entry.key = key;
                entry.color = color;
                colors.add(entry);
            }
            polygons.add(new ProjectedPolygon(polygon->area, color));
        }
        for ( long i = 0; i < visible.size(); i++ ) {
            delete visible[i];
        }
    }

    // C++ port: the boundary representations and their bodies were only for
    // this view
    for ( long i = 0; i < surrogates.size(); i++ ) {
        delete surrogates[i];
    }
    surrogates.clear();
    brepCache.endFrame();

    if ( cached != nullptr ) {
        delete cached;
        cache.remove(cachedIndex);
    }
    cached = new CachedView();
    cached->viewport = viewport;
    cached->cameraState = cameraState;
    cached->width = width;
    cached->height = height;
    cached->generation = generation;
    cached->withShadows = withShadows;
    cached->polygons = polygons;
    cache.add(cached);
    return cached->polygons;
}

/**
@param lights lights reaching the polygon
@return the flat shaded color of the face of a visible polygon, under the
given lights (without lights in the scene, under a light at the eye)
*/
ColorRgb ProjectedSurfacesViewBuilder::shadeFace(
    HiddenSurfaceRenderer::VisiblePolygon* polygon, Scene* scene,
    Camera* camera, java::ArrayList<Light*>& lights)
{
    (void)scene;
    SimpleMaterial* material = polygon->body->getMaterial();
    SimpleMaterial defaultMaterial = Scene::defaultMaterial();
    Vector3Dd normal = polygon->worldPlane.getNormal();
    Vector3Dd center = faceCenter(polygon->face, polygon->body);
    Vector3Dd toEye;

    if ( material == nullptr ) {
        material = &defaultMaterial;
    }
    if ( camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        toEye = camera->getFront().multiply(-1.0).normalized();
    }
    else {
        toEye = camera->getPosition().subtract(center).normalized();
    }
    if ( polygon->lights == nullptr && lights.isEmpty() ) {
        // Without lights, a light at the eye
        double factor = std::fabs(normal.dotProduct(toEye));
        ColorRgb diffuse = material->getDiffuse();
        return ColorRgb(NO_LIGHT_COLOR.r() * diffuse.r() * factor,
            NO_LIGHT_COLOR.g() * diffuse.g() * factor,
            NO_LIGHT_COLOR.b() * diffuse.b() * factor);
    }

    RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL, false);
    hit.point = center;
    hit.normal = normal;
    java::ArrayList<SimpleBody*> noBodies;
    ColorRgb color = shader.shadeLocal(&hit, toEye.x(), toEye.y(), toEye.z(),
        lights, noBodies, material, &workspace).color;
    return ColorRgb(clamp(color.r()), clamp(color.g()), clamp(color.b()));
}

/**
@return the average of the vertices of a face, in world coordinates
*/
Vector3Dd ProjectedSurfacesViewBuilder::faceCenter(
    _PolyhedralBoundedSolidFace* face, SimpleBody* body)
{
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    int n = 0;

    for ( long i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* heStart = he;
        do {
            if ( he == nullptr || he->startingVertex == nullptr ) {
                break;
            }
            x += he->startingVertex->position.x();
            y += he->startingVertex->position.y();
            z += he->startingVertex->position.z();
            n++;
            he = he->next();
        } while ( he != heStart );
    }
    if ( n == 0 ) {
        return body->getPosition();
    }
    return body->getTransformationMatrix().multiply(
        Vector3Dd(x / n, y / n, z / n));
}

double ProjectedSurfacesViewBuilder::clamp(double value)
{
    return java::Math::max(0.0, java::Math::min(1.0, value));
}

/**
A body whose geometry is exported to a `PolyhedralBoundedSolid` (i.e. a
`Box` or a `Sphere`) is replaced, for the hidden surface algorithm, by a
body with the same transformation and material, and the exported
boundary representation as geometry.
*/
SimpleBody* ProjectedSurfacesViewBuilder::brepBody(SimpleBody* body,
                                                   PolyhedralBoundedSolid* brep)
{
    Geometry* geometry = body->getGeometry();
    if ( geometry == brep ) {
        return body;
    }
    SimpleBody* surrogate = new SimpleBody();
    surrogate->setName(body->getName());
    surrogate->setGeometryReference(brep);
    surrogate->setPosition(body->getPosition());
    surrogate->setRotation(body->getRotation());
    surrogate->setScale(body->getScale());
    surrogate->setMaterialReference(body->getMaterial());
    surrogates.add(surrogate);
    return surrogate;
}
