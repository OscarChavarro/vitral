#include <cmath>

#include "java/util/ArrayList.txx"
#include "model/Scene.h"
#include "model/selection/SelectionSet.h"
#include "render/BodyEditFeedbackProvider.h"
#include "render/ClipSpaceLineProjector.h"
#include "render/ProjectedLinesViewBuilder.h"
#include "render/RenderPrimitive.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryCalligraphicExporter.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryTriangulator.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/media/Calligraphic3DBuffer.h"
#include "vsdk/toolkit/render/SelectionCorners.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"

namespace {
const ColorRgb GRID_COLOR(0.37, 0.37, 0.37);
const ColorRgb GRID_AXIS_COLOR(0, 0, 0);
const ColorRgb VISIBLE_LINE_COLOR(0, 0, 0);
const ColorRgb HIDDEN_LINE_COLOR(0.32, 0.32, 0.32);
const ColorRgb BOUNDING_VOLUME_COLOR(0.6, 0.9, 1.0);
const ColorRgb POINT_COLOR(1.0, 0.0, 0.0);
const ColorRgb LIGHT_COLOR(1.0, 1.0, 1.0);
const ColorRgb SELECTED_LIGHT_COLOR(1.0, 1.0, 0.0);
const ColorRgb DEBUG_COLOR(0.2, 0.8, 1.0);
const ColorRgb EDIT_FEEDBACK_COLOR(1.0, 0.55, 0.0);
const double LIGHT_GIZMO_SIZE = 0.15;
/// Distance an orthogonal camera is moved back for the Appel lines of sight
const double ORTHOGONAL_EYE_DISTANCE = 1000.0;
/// Bounds of geometries without a finite bounding box (i.e. planes)
const double UNBOUNDED_LIMIT = 1e300;

ColorRgb wireColor(SimpleMaterial* material)
{
    if ( material == nullptr ) {
        return VISIBLE_LINE_COLOR;
    }
    return material->getDiffuse();
}

double toDegrees(double radians) { return radians * 180.0 / M_PI; }
double toRadians(double degrees) { return degrees * M_PI / 180.0; }

/**
The quantitative invisibility of [APPE1967] is measured along the line of
sight from the eye to each point. For an orthogonal camera all the lines of
sight are parallel, so the eye is moved far back along the viewing direction
(with its far plane, so the projection does not change).
*/
Camera lineOfSightCamera(const Camera* camera)
{
    Camera eye(*camera);
    if ( camera->getProjectionMode() != Camera::PROJECTION_MODE_ORTHOGONAL ) {
        return eye;
    }
    Vector3Dd front = camera->getFront().normalized();
    eye.setPosition(camera->getPosition().subtract(
        front.multiply(ORTHOGONAL_EYE_DISTANCE)));
    eye.setNearPlaneDistance(
        camera->getNearPlaneDistance() + ORTHOGONAL_EYE_DISTANCE);
    eye.setFarPlaneDistance(
        camera->getFarPlaneDistance() + ORTHOGONAL_EYE_DISTANCE);
    return eye;
}

/**
The point of a plane viewed by a camera: where the line of sight through the
center of the view meets the plane, or, if it does not meet it in front of
the camera and before its far plane, the point of the plane closest to the
camera.
@return the point, in the coordinates of the plane
*/
Vector3Dd viewedPoint(const InfinitePlane& plane, const Matrix4x4d& transform,
                      const Camera* camera)
{
    Matrix4x4d inverse = transform.inverse();
    Vector3Dd eye = inverse.multiply(camera->getPosition());
    Vector3Dd direction = inverse.multiply(
        camera->getPosition().add(camera->getFront())).subtract(eye);
    double towards = plane.getCoefficients().dotProduct(direction);
    double length = direction.length();

    if ( towards != 0 && length > 0 ) {
        double t = -plane.pointDistance(eye) / towards;
        if ( t > 0 && t * length <= camera->getFarPlaneDistance() ) {
            return eye.add(direction.multiply(t));
        }
    }
    return plane.projectPoint(eye);
}
}

struct ProjectedLinesViewBuilder::FreeLineSet {
    Calligraphic3DBuffer lines;
    ColorRgb color;

    explicit FreeLineSet(const ColorRgb& color) : color(color) {}
};

ProjectedLinesViewBuilder::~ProjectedLinesViewBuilder()
{
    for ( long i = 0; i < surrogates.size(); i++ ) {
        delete surrogates.get(i);
    }
    surrogates.clear();
}

void ProjectedLinesViewBuilder::buildView(Scene* scene, Viewport* viewport,
    BodyEditFeedbackProvider* editor, bool withBodies,
    java::ArrayList<ProjectedLineLayer*>& layers)
{
    Camera* camera = viewport->getActiveCamera();
    RendererConfiguration* quality = viewport->getRendererConfiguration();
    ClipSpaceLineProjector projector(camera);

    if ( withBodies ) {
        java::ArrayList<FreeLineSet*> freeLines;
        if ( viewport->isShowGrid() ) {
            addGrid(freeLines, camera);
        }
        addBodies(layers, freeLines, projector, scene, camera, quality);
        for ( long i = 0; i < freeLines.size(); i++ ) {
            delete freeLines.get(i);
        }
    }
    addAnnotations(layers, projector, camera, scene, quality, editor);
    addDebugEntities(layers, projector, camera, scene);
    addLights(layers, projector, scene);

    // The boundary representations and their bodies were only for this view
    for ( long i = 0; i < surrogates.size(); i++ ) {
        delete surrogates.get(i);
    }
    surrogates.clear();
    brepCache.endFrame();
}

//= Grid ==================================================================

/**
Same reference grid as the OpenGL presentation: 14x14 unit cells on the
plane z = 0, or on the vertical plane facing the camera for orthogonal side
views. Its lines are free lines (the solids hide them).
*/
void ProjectedLinesViewBuilder::addGrid(java::ArrayList<FreeLineSet*>& freeLines,
                                        Camera* camera)
{
    Matrix4x4d gridTransform;
    Matrix4x4d rotation = camera->getRotation();
    double yaw = toDegrees(rotation.obtainEulerYawAngle());
    double pitch = toDegrees(rotation.obtainEulerPitchAngle());

    if ( camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL &&
         (pitch > -45 && pitch < 45) ) {
        if ( (yaw > 45 && yaw < 135) || (yaw < -45 && yaw > -135) ) {
            gridTransform = gridTransform.axisRotation(toRadians(90), 1, 0, 0);
        }
        else {
            gridTransform = gridTransform.axisRotation(toRadians(90), 0, 1, 0);
        }
    }

    FreeLineSet* grid = new FreeLineSet(GRID_COLOR);
    FreeLineSet* axes = new FreeLineSet(GRID_AXIS_COLOR);
    int n = 14;
    double half = n / 2.0;

    for ( int i = 0; i <= n; i++ ) {
        double c = -half + i;
        FreeLineSet* target = (i == n / 2) ? axes : grid;
        target->lines.add3DLine(gridTransform.multiply(Vector3Dd(c, -half, 0)),
            gridTransform.multiply(Vector3Dd(c, half, 0)));
        target->lines.add3DLine(gridTransform.multiply(Vector3Dd(-half, c, 0)),
            gridTransform.multiply(Vector3Dd(half, c, 0)));
    }
    freeLines.add(grid);
    freeLines.add(axes);
}

//= Bodies ================================================================

void ProjectedLinesViewBuilder::addBodies(
    java::ArrayList<ProjectedLineLayer*>& layers,
    java::ArrayList<FreeLineSet*>& freeLines,
    const ClipSpaceLineProjector& projector, Scene* scene, Camera* camera,
    RendererConfiguration* quality)
{
    java::ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    java::ArrayList<SimpleBody*> solids;
    long firstBodyLayer = layers.size();
    ProjectedLineLayer* points = new ProjectedLineLayer(POINT_COLOR, 3.0,
        ProjectedLineLayer::POINTS);

    for ( long i = 0; i < bodies.size(); i++ ) {
        SimpleBody* body = bodies.get(i);
        if ( body == nullptr || scene->isHidden(body) ) {
            continue;
        }
        Geometry* geometry = body->getGeometry();
        if ( geometry == nullptr ) {
            continue;
        }
        Matrix4x4d transform = body->getTransformationMatrix();
        PolyhedralBoundedSolid* brep = brepCache.get(geometry);
        Calligraphic3DBuffer* lines = nullptr;
        if ( quality->isSurfacesSet() && brep == nullptr ) {
            lines = exportToLines(geometry, transform, camera);
        }
        if ( quality->isSurfacesSet() && brep != nullptr ) {
            // Drawn below, all at once: they occlude each other
            solids.add(brepBody(body, brep));
        }
        else if ( lines != nullptr ) {
            // Curves and surfaces: hidden by the solids, hide nothing
            addFreeLines(freeLines, lines, body);
        }
        else {
            // Without surfaces (or without a boundary representation to
            // hide with), a plain wireframe, whatever the wires flag is
            ProjectedLineLayer* wires = new ProjectedLineLayer(
                wireColor(body->getMaterial()), 1.0, ProjectedLineLayer::SOLID);
            addWireframe(wires, projector, camera, geometry, brep, transform);
            addIfNotEmpty(layers, wires);
        }
        delete lines;
        if ( quality->isPointsSet() ) {
            addPoints(points, projector, geometry, brep, transform);
        }
    }

    if ( solids.size() > 0 ) {
        addHiddenLineDrawing(layers, solids, freeLines, camera,
                             quality->isWiresSet());
    }
    else {
        addFreeLinesWithoutHiding(layers, firstBodyLayer, freeLines, projector);
    }
    addIfNotEmpty(layers, points);
}

/**
The lines of a curve or a surface, in world coordinates, with the wire color
of its body.
@param local the lines, in the coordinates of the body
*/
void ProjectedLinesViewBuilder::addFreeLines(
    java::ArrayList<FreeLineSet*>& freeLines, Calligraphic3DBuffer* local,
    SimpleBody* body)
{
    Matrix4x4d transform = body->getTransformationMatrix();
    FreeLineSet* set = new FreeLineSet(wireColor(body->getMaterial()));
    Vector3Dd p0;
    Vector3Dd p1;

    for ( int i = 0; i < local->getNumLines(); i++ ) {
        local->get3DLine(i, p0, p1);
        set->lines.add3DLine(transform.multiply(p0), transform.multiply(p1));
    }
    freeLines.add(set);
}

/**
Without solids to hide them, the free lines are simply projected, below the
wireframes of the bodies.
@param position index in `layers` where their layers are inserted
*/
void ProjectedLinesViewBuilder::addFreeLinesWithoutHiding(
    java::ArrayList<ProjectedLineLayer*>& layers, long position,
    java::ArrayList<FreeLineSet*>& freeLines,
    const ClipSpaceLineProjector& projector)
{
    Vector3Dd p0;
    Vector3Dd p1;

    for ( long s = 0; s < freeLines.size(); s++ ) {
        FreeLineSet* set = freeLines.get(s);
        ProjectedLineLayer* layer = new ProjectedLineLayer(set->color, 1.0,
            ProjectedLineLayer::SOLID);
        for ( int i = 0; i < set->lines.getNumLines(); i++ ) {
            set->lines.get3DLine(i, p0, p1);
            projector.addSegment(layer->getLines(), p0, p1);
        }
        if ( !layer->isEmpty() ) {
            layers.add(position, layer);
            position++;
        }
        else {
            delete layer;
        }
    }
}

/**
Presents a set of solids with the hidden line removal of [APPE1967], and the
free lines hidden by them. The hidden parts of all the lines are drawn first
(dashed, only if asked for), then the visible free lines and finally the
visible lines of the solids.
*/
void ProjectedLinesViewBuilder::addHiddenLineDrawing(
    java::ArrayList<ProjectedLineLayer*>& layers,
    java::ArrayList<SimpleBody*>& solids,
    java::ArrayList<FreeLineSet*>& freeLines,
    Camera* camera, bool withHiddenLines)
{
    ProjectedLineLayer* contours = new ProjectedLineLayer(VISIBLE_LINE_COLOR,
        2.0, ProjectedLineLayer::SOLID);
    ProjectedLineLayer* visible = new ProjectedLineLayer(VISIBLE_LINE_COLOR,
        1.0, ProjectedLineLayer::SOLID);
    ProjectedLineLayer* hidden = new ProjectedLineLayer(HIDDEN_LINE_COLOR,
        1.0, ProjectedLineLayer::DASHED);

    java::ArrayList<Calligraphic3DBuffer*> freeInput;
    java::ArrayList<Calligraphic2DBuffer*> freeVisibleOutput;
    java::ArrayList<Calligraphic2DBuffer*> freeHiddenOutput;
    java::ArrayList<ProjectedLineLayer*> freeVisible;
    java::ArrayList<ProjectedLineLayer*> freeHidden;

    for ( long s = 0; s < freeLines.size(); s++ ) {
        FreeLineSet* set = freeLines.get(s);
        ProjectedLineLayer* setVisible = new ProjectedLineLayer(set->color,
            1.0, ProjectedLineLayer::SOLID);
        ProjectedLineLayer* setHidden = new ProjectedLineLayer(set->color,
            1.0, ProjectedLineLayer::DASHED);
        freeInput.add(&set->lines);
        freeVisibleOutput.add(setVisible->getLines());
        freeHiddenOutput.add(setHidden->getLines());
        freeVisible.add(setVisible);
        freeHidden.add(setHidden);
    }

    Camera eye = lineOfSightCamera(camera);
    HiddenLineRenderer::executeAppelAlgorithm(solids, &eye,
        contours->getLines(), visible->getLines(), hidden->getLines(),
        freeInput, freeVisibleOutput, freeHiddenOutput);

    if ( withHiddenLines ) {
        for ( long i = 0; i < freeHidden.size(); i++ ) {
            addIfNotEmpty(layers, freeHidden.get(i));
        }
        addIfNotEmpty(layers, hidden);
    }
    else {
        ProjectedLineLayer::deleteAll(freeHidden);
        delete hidden;
    }
    for ( long i = 0; i < freeVisible.size(); i++ ) {
        addIfNotEmpty(layers, freeVisible.get(i));
    }
    addIfNotEmpty(layers, visible);
    addIfNotEmpty(layers, contours);
}

/**
The quantitative invisibility is answered by the geometry of each body, and
only a `PolyhedralBoundedSolid` answers it: a body whose geometry is
exported to one (i.e. a `Box` or a `Sphere`) is replaced, for the hidden
line algorithm, by a body with the same transformation and the exported
boundary representation as geometry.
*/
SimpleBody* ProjectedLinesViewBuilder::brepBody(SimpleBody* body,
                                                PolyhedralBoundedSolid* brep)
{
    if ( body->getGeometry() == brep ) {
        return body;
    }
    SimpleBody* surrogate = new SimpleBody();
    surrogate->setName(body->getName());
    surrogate->setGeometryReference(brep);
    surrogate->setPosition(body->getPosition());
    surrogate->setRotation(body->getRotation());
    surrogate->setScale(body->getScale());
    surrogates.add(surrogate);
    return surrogate;
}

/**
@return the lines of a curve or of the mesh of a surface, in the
coordinates of the geometry (see `GeometryCalligraphicExporter`), owned by
the caller, or null if it has none
*/
Calligraphic3DBuffer* ProjectedLinesViewBuilder::exportToLines(
    Geometry* geometry, const Matrix4x4d& transform, Camera* camera)
{
    InfinitePlane* plane = dynamic_cast<InfinitePlane*>(geometry);
    if ( plane != nullptr ) {
        return GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(
            *plane, viewedPoint(*plane, transform, camera));
    }
    return GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(geometry);
}

/**
Adds the wireframe of a geometry: the edges of its boundary representation,
else the lines of a curve or of the mesh of a surface
(`GeometryCalligraphicExporter`), else its bounding box.
*/
void ProjectedLinesViewBuilder::addWireframe(ProjectedLineLayer* layer,
    const ClipSpaceLineProjector& projector, Camera* camera,
    Geometry* geometry, PolyhedralBoundedSolid* brep,
    const Matrix4x4d& transform)
{
    if ( brep != nullptr ) {
        CircularArrayList<_PolyhedralBoundedSolidEdge*>& edges =
            brep->getEdgesList();
        for ( long i = 0; i < edges.size(); i++ ) {
            _PolyhedralBoundedSolidEdge* edge = edges.get(i);
            if ( edge == nullptr ||
                 edge->getStartingVertexId() < 0 ||
                 edge->getEndingVertexId() < 0 ||
                 edge->rightHalf == nullptr || edge->leftHalf == nullptr ||
                 edge->rightHalf->startingVertex == nullptr ||
                 edge->leftHalf->startingVertex == nullptr ) {
                continue;
            }
            projector.addSegment(layer->getLines(), transform,
                edge->rightHalf->startingVertex->position,
                edge->leftHalf->startingVertex->position);
        }
        return;
    }

    Calligraphic3DBuffer* lines = exportToLines(geometry, transform, camera);
    if ( lines != nullptr ) {
        Vector3Dd p0;
        Vector3Dd p1;
        for ( int i = 0; i < lines->getNumLines(); i++ ) {
            lines->get3DLine(i, p0, p1);
            projector.addSegment(layer->getLines(), transform, p0, p1);
        }
        delete lines;
        return;
    }

    double* minmax = geometry->getMinMax();
    addBox(layer, projector, minmax, transform);
    delete[] minmax;
}

void ProjectedLinesViewBuilder::addPoints(ProjectedLineLayer* layer,
    const ClipSpaceLineProjector& projector, Geometry* geometry,
    PolyhedralBoundedSolid* brep, const Matrix4x4d& transform)
{
    if ( brep != nullptr ) {
        CircularArrayList<_PolyhedralBoundedSolidVertex*>& vertices =
            brep->getVerticesList();
        for ( long i = 0; i < vertices.size(); i++ ) {
            _PolyhedralBoundedSolidVertex* vertex = vertices.get(i);
            if ( vertex != nullptr ) {
                projector.addPoint(layer->getLines(),
                                   transform.multiply(vertex->position));
            }
        }
        return;
    }

    TriangleMeshGroup meshes;
    if ( !GeometryTriangulator::exportToTriangleMeshGroup(geometry, meshes) ) {
        return;
    }
    java::ArrayList<TriangleMesh>& list = meshes.getMeshes();
    for ( long m = 0; m < list.size(); m++ ) {
        TriangleMesh& mesh = list[m];
        java::ArrayList<double>& v = mesh.getVertexPositions();
        for ( int i = 0; i < mesh.getNumVertices() && 3*i + 2 < v.size(); i++ ) {
            projector.addPoint(layer->getLines(), transform.multiply(
                Vector3Dd(v.get(3*i), v.get(3*i + 1), v.get(3*i + 2))));
        }
    }
}

//= Annotations ===========================================================

void ProjectedLinesViewBuilder::addAnnotations(
    java::ArrayList<ProjectedLineLayer*>& layers,
    const ClipSpaceLineProjector& projector, Camera* camera, Scene* scene,
    RendererConfiguration* quality, BodyEditFeedbackProvider* editor)
{
    java::ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    ProjectedLineLayer* boxes = new ProjectedLineLayer(BOUNDING_VOLUME_COLOR,
        1.0, ProjectedLineLayer::SOLID);
    ProjectedLineLayer* corners = new ProjectedLineLayer(
        SelectionCorners::getDefaultColor(), 1.0, ProjectedLineLayer::SOLID);

    scene->selectedThings->sync();
    for ( long i = 0; i < bodies.size(); i++ ) {
        SimpleBody* body = bodies.get(i);
        if ( body == nullptr || scene->isHidden(body) ) {
            continue;
        }
        Geometry* geometry = body->getGeometry();
        if ( geometry == nullptr ) {
            continue;
        }
        bool selected = scene->selectedThings->isSelected(static_cast<int>(i));
        if ( !quality->isBoundingVolumeSet() && !selected ) {
            continue;
        }
        double* minmax = geometry->getMinMax();
        Matrix4x4d transform = body->getTransformationMatrix();
        if ( quality->isBoundingVolumeSet() ) {
            addBox(boxes, projector, minmax, transform);
        }
        if ( selected ) {
            addSelectionCorners(corners, projector, minmax, transform);
        }
        delete[] minmax;
    }
    addIfNotEmpty(layers, boxes);
    addIfNotEmpty(layers, corners);

    if ( editor != nullptr && editor->getTarget() != nullptr ) {
        ProjectedLineLayer* feedback = new ProjectedLineLayer(
            EDIT_FEEDBACK_COLOR, 1.0, ProjectedLineLayer::SOLID);
        java::ArrayList<RenderPrimitive> primitives =
            editor->buildEditFeedback();
        for ( long i = 0; i < primitives.size(); i++ ) {
            Geometry* geometry = primitives[i].getGeometry();
            if ( geometry != nullptr ) {
                addWireframe(feedback, projector, camera, geometry,
                    brepCache.get(geometry), primitives[i].getTransform());
            }
        }
        addIfNotEmpty(layers, feedback);
    }
}

void ProjectedLinesViewBuilder::addSelectionCorners(ProjectedLineLayer* layer,
    const ClipSpaceLineProjector& projector, const double* minmax,
    const Matrix4x4d& transform)
{
    java::ArrayList<Vector3Dd> segments =
        SelectionCorners::buildSegments(minmax);

    for ( long i = 0; i + 1 < segments.size(); i += 2 ) {
        projector.addSegment(layer->getLines(), transform, segments.get(i),
                             segments.get(i + 1));
    }
}

/**
Adds the twelve edges of an axis aligned box.
@param minmax minimum x, y, z followed by maximum x, y, z
*/
void ProjectedLinesViewBuilder::addBox(ProjectedLineLayer* layer,
    const ClipSpaceLineProjector& projector, const double* minmax,
    const Matrix4x4d& transform)
{
    if ( minmax == nullptr ) {
        return;
    }
    for ( int i = 0; i < 6; i++ ) {
        // i.e. an infinite plane
        if ( std::isinf(minmax[i]) || std::isnan(minmax[i]) ||
             std::fabs(minmax[i]) >= UNBOUNDED_LIMIT ) {
            return;
        }
    }
    Vector3Dd c[8];

    for ( int i = 0; i < 8; i++ ) {
        c[i] = Vector3Dd(
            (i & 1) != 0 ? minmax[3] : minmax[0],
            (i & 2) != 0 ? minmax[4] : minmax[1],
            (i & 4) != 0 ? minmax[5] : minmax[2]);
    }
    for ( int i = 0; i < 8; i++ ) {
        for ( int bit = 1; bit < 8; bit <<= 1 ) {
            if ( (i & bit) == 0 ) {
                projector.addSegment(layer->getLines(), transform, c[i],
                                     c[i | bit]);
            }
        }
    }
}

//= Debug entities and lights =============================================

void ProjectedLinesViewBuilder::addDebugEntities(
    java::ArrayList<ProjectedLineLayer*>& layers,
    const ClipSpaceLineProjector& projector, Camera* camera, Scene* scene)
{
    ProjectedLineLayer* debug = new ProjectedLineLayer(DEBUG_COLOR, 1.0,
        ProjectedLineLayer::SOLID);

    for ( long g = 0; g < scene->debugThingGroups.size(); g++ ) {
        SimpleBodyGroup* group = scene->debugThingGroups.get(g);
        if ( group == nullptr ) {
            continue;
        }
        Matrix4x4d groupTransform = group->getTransformationMatrix();
        java::ArrayList<SimpleBody*>& bodies = group->getBodies();
        for ( long i = 0; i < bodies.size(); i++ ) {
            SimpleBody* body = bodies.get(i);
            Geometry* geometry = body != nullptr ? body->getGeometry() : nullptr;
            if ( geometry != nullptr ) {
                addWireframe(debug, projector, camera, geometry,
                    brepCache.get(geometry),
                    groupTransform.multiply(body->getTransformationMatrix()));
            }
        }
    }
    addIfNotEmpty(layers, debug);
}

/**
Each light is presented as a small star of three axis aligned segments and
four diagonals, centered at its position.
*/
void ProjectedLinesViewBuilder::addLights(
    java::ArrayList<ProjectedLineLayer*>& layers,
    const ClipSpaceLineProjector& projector, Scene* scene)
{
    java::ArrayList<Light*>& lights = scene->scene->getLights();
    ProjectedLineLayer* normal = new ProjectedLineLayer(LIGHT_COLOR, 1.0,
        ProjectedLineLayer::SOLID);
    ProjectedLineLayer* selected = new ProjectedLineLayer(SELECTED_LIGHT_COLOR,
        2.0, ProjectedLineLayer::SOLID);
    double s = LIGHT_GIZMO_SIZE * scene->getLightGizmoScale();
    double d = s / std::sqrt(3.0);
    const Vector3Dd directions[7] = {
        Vector3Dd(s, 0, 0), Vector3Dd(0, s, 0), Vector3Dd(0, 0, s),
        Vector3Dd(d, d, d), Vector3Dd(-d, d, d),
        Vector3Dd(d, -d, d), Vector3Dd(d, d, -d)
    };

    scene->selectedLights->sync();
    for ( long i = 0; i < lights.size(); i++ ) {
        Light* light = lights.get(i);
        if ( light == nullptr ) {
            continue;
        }
        Vector3Dd center = light->getPosition();
        ProjectedLineLayer* target =
            scene->selectedLights->isSelected(static_cast<int>(i)) ?
            selected : normal;
        for ( int k = 0; k < 7; k++ ) {
            projector.addSegment(target->getLines(),
                center.subtract(directions[k]), center.add(directions[k]));
        }
    }
    addIfNotEmpty(layers, normal);
    addIfNotEmpty(layers, selected);
}

void ProjectedLinesViewBuilder::addIfNotEmpty(
    java::ArrayList<ProjectedLineLayer*>& layers, ProjectedLineLayer* layer)
{
    if ( !layer->isEmpty() ) {
        layers.add(layer);
    }
    else {
        delete layer;
    }
}
