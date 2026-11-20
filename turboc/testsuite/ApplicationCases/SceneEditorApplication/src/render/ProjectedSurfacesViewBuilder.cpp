//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#include "render/ProjectedSurfacesViewBuilder.h"

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "model/Scene.h"
#include "render/BrepExportCache.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
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

static const ColorRgb NO_LIGHT_COLOR(0.8, 0.8, 0.8);

_ProjectedSurfacesCachedView::_ProjectedSurfacesCachedView()
    : viewport(NULL), cameraState(NULL), width(0), height(0), generation(0),
      withShadows(FALSE), polygons()
{
}

_ProjectedSurfacesCachedView::~_ProjectedSurfacesCachedView()
{
    long i;

    for ( i = 0; i < polygons.size(); i++ ) {
        delete polygons[i];
    }
    delete cameraState;
}

ProjectedSurfacesViewBuilder::ProjectedSurfacesViewBuilder()
    : brepCache(new BrepExportCache()), cache(), shader(), workspace()
{
}

ProjectedSurfacesViewBuilder::~ProjectedSurfacesViewBuilder()
{
    long i;

    for ( i = 0; i < cache.size(); i++ ) {
        delete cache[i];
    }
    delete brepCache;
}

/**
@return true when both lists hold the same lights, in the same order
*/
static BOOLEAN sameLights(const ArrayList<Light*>& a, const ArrayList<Light*>& b)
{
    long i;

    if ( a.size() != b.size() ) {
        return FALSE;
    }
    for ( i = 0; i < a.size(); i++ ) {
        if ( a[i] != b[i] ) {
            return FALSE;
        }
    }
    return TRUE;
}

const ArrayList<ProjectedPolygon*>& ProjectedSurfacesViewBuilder::buildView(
    Scene* scene, Viewport* viewport, int width, int height, long generation,
    BOOLEAN withShadows)
{
    Camera* camera = viewport->getActiveCamera();
    CameraState* cameraState = CameraState::capture(camera);
    _ProjectedSurfacesCachedView* cached = NULL;
    long i;
    long j;

    for ( i = 0; i < cache.size(); i++ ) {
        if ( cache[i]->viewport == viewport ) {
            cached = cache[i];
            break;
        }
    }

    if ( cached != NULL && cached->width == width &&
         cached->height == height && cached->generation == generation &&
         cached->withShadows == withShadows &&
         cached->cameraState->isSameState(cameraState) ) {
        delete cameraState;
        return cached->polygons;
    }

    // C++ port note: the surrogate bodies and the solids exported for the
    // caller live while the polygons are built
    ArrayList<SimpleBody*> solids;
    ArrayList<SimpleBody*> surrogates;
    ArrayList<PolyhedralBoundedSolid*> exported;
    ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    for ( i = 0; i < bodies.size(); i++ ) {
        SimpleBody* body = bodies[i];
        if ( scene->isHidden(body) || body->getGeometry() == NULL ) {
            continue;
        }
        BOOLEAN callerOwns = FALSE;
        PolyhedralBoundedSolid* brep = brepCache->get(body->getGeometry(),
                                                      callerOwns);
        if ( brep != NULL && callerOwns ) {
            exported.add(brep);
        }
        if ( brep != NULL ) {
            SimpleBody* solid = brepBody(body, brep);
            if ( solid != body ) {
                surrogates.add(solid);
            }
            solids.add(solid);
        }
    }

    ArrayList<ProjectedPolygon*> polygons;
    ArrayList<Light*>& lights = scene->scene->getLights();
    BOOLEAN shadows = withShadows && !lights.isEmpty();
    if ( !solids.isEmpty() ) {
        ArrayList<HiddenSurfaceRenderer::VisiblePolygon*> visible;
        if ( shadows ) {
            visible = HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(
                solids, camera, lights);
        }
        else {
            visible = HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(
                solids, camera);
        }
        // The color depends on the face and, with shadows, on the
        // lights reaching the polygon
        ArrayList<_ProjectedSurfacesColorKey*> colors;
        ArrayList<Light*> noLights;
        for ( i = 0; i < visible.size(); i++ ) {
            HiddenSurfaceRenderer::VisiblePolygon* polygon = visible[i];
            ArrayList<Light*>& reaching = polygon->lights != NULL ?
                *polygon->lights : noLights;
            _ProjectedSurfacesColorKey* key = NULL;
            for ( j = 0; j < colors.size(); j++ ) {
                if ( colors[j]->face == polygon->face &&
                     colors[j]->body == polygon->body &&
                     sameLights(colors[j]->lights, reaching) ) {
                    key = colors[j];
                    break;
                }
            }
            if ( key == NULL ) {
                key = new _ProjectedSurfacesColorKey();
                key->face = polygon->face;
                key->body = polygon->body;
                key->lights.addAll(reaching);
                if ( polygon->lights != NULL ) {
                    key->color = shadeFace(polygon, scene, camera,
                                           *polygon->lights);
                }
                else {
                    key->color = shadeFace(polygon, scene, camera, lights);
                }
                colors.add(key);
            }
            polygons.add(new ProjectedPolygon(polygon->area, key->color));
            // The area now belongs to the projected polygon
            polygon->area = NULL;
            delete polygon;
        }
        for ( j = 0; j < colors.size(); j++ ) {
            delete colors[j];
        }
    }
    for ( i = 0; i < surrogates.size(); i++ ) {
        delete surrogates[i];
    }
    for ( i = 0; i < exported.size(); i++ ) {
        delete exported[i];
    }

    if ( cached == NULL ) {
        cached = new _ProjectedSurfacesCachedView();
        cached->viewport = viewport;
        cache.add(cached);
    }
    else {
        for ( i = 0; i < cached->polygons.size(); i++ ) {
            delete cached->polygons[i];
        }
        cached->polygons.clear();
        delete cached->cameraState;
    }
    cached->cameraState = cameraState;
    cached->width = width;
    cached->height = height;
    cached->generation = generation;
    cached->withShadows = withShadows;
    cached->polygons.addAll(polygons);
    return cached->polygons;
}

/**
@param lights lights reaching the polygon
@return the flat shaded color of the face of a visible polygon, under the
given lights (without lights in the scene, under a light at the eye)
*/
ColorRgb ProjectedSurfacesViewBuilder::shadeFace(
    HiddenSurfaceRenderer::VisiblePolygon* polygon, Scene* scene,
    Camera* camera, ArrayList<Light*>& lights)
{
    SimpleMaterial* material = polygon->body->getMaterial();
    // C++ port note: a copy of the default material of the scene, used
    // when the body has none
    SimpleMaterial defaultMaterial(Scene::defaultMaterial());
    Vector3Dd normal = polygon->worldPlane->getNormal();
    Vector3Dd center = faceCenter(polygon->face, polygon->body);
    Vector3Dd toEye;

    (void)scene;
    if ( material == NULL ) {
        material = &defaultMaterial;
    }
    if ( camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        toEye = camera->getFront().multiply(-1.0).normalized();
    }
    else {
        toEye = camera->getPosition().subtract(center).normalized();
    }
    if ( polygon->lights == NULL && lights.isEmpty() ) {
        // Without lights, a light at the eye
        double factor = Math::abs(normal.dotProduct(toEye));
        ColorRgb diffuse = material->getDiffuse();
        return ColorRgb(NO_LIGHT_COLOR.r() * diffuse.r() * factor,
            NO_LIGHT_COLOR.g() * diffuse.g() * factor,
            NO_LIGHT_COLOR.b() * diffuse.b() * factor);
    }

    RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL, FALSE);
    hit.point = center;
    hit.normal = normal;
    ArrayList<SimpleBody*> noBodies;
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
    long i;

    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if ( loop == NULL || loop->boundaryStartHalfEdge == NULL ) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* heStart = he;
        do {
            if ( he == NULL || he->startingVertex == NULL ) {
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
    return Math::max(0.0, Math::min(1.0, value));
}

/**
A body whose geometry is exported to a `PolyhedralBoundedSolid` (i.e. a
`Box` or a `Sphere`) is replaced, for the hidden surface algorithm, by a
body with the same transformation and material, and the exported
boundary representation as geometry.
C++ port note: the surrogate body, owned by the caller, owns neither the
solid nor the material.
*/
SimpleBody* ProjectedSurfacesViewBuilder::brepBody(SimpleBody* body,
    PolyhedralBoundedSolid* brep)
{
    Geometry* geometry = body->getGeometry();
    if ( geometry == (Geometry*)brep ) {
        return body;
    }
    SimpleBody* surrogate = new SimpleBody();
    surrogate->setName(body->getName());
    surrogate->setGeometryReference(brep);
    surrogate->setPosition(body->getPosition());
    surrogate->setRotation(body->getRotation());
    surrogate->setScale(body->getScale());
    surrogate->setMaterialReference(body->getMaterial());
    return surrogate;
}
