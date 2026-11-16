#include <cmath>

#include "java/util/ArrayList.txx"
#include "gui/CameraFaceFocusInteraction.h"

#include "java/lang/Math.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/gui/CameraControllerOrbiter.h"
#include "models/DebuggerModel.h"

static const double SAFETY_MARGIN = 1.05;

bool CameraFaceFocusInteraction::focusSelectedFace(DebuggerModel& model)
{
    PolyhedralBoundedSolid* solid = model.getSolid();
    int faceIndex = model.getFaceIndex();
    Camera* camera = model.getCamera();

    if ( solid == nullptr || camera == nullptr ) {
        return false;
    }
    if ( faceIndex < 0 || faceIndex >= (int)solid->getPolygonsList().size() ) {
        return false;
    }

    _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(faceIndex);
    java::ArrayList<_PolyhedralBoundedSolidVertex*> faceVertices =
        collectFaceVertices(face);
    if ( faceVertices.isEmpty() ) {
        return false;
    }

    Vector3Dd center = computeCentroid(faceVertices);
    // C++ port note: `getContainingPlane` returns a new plane
    InfinitePlane* plane = face->getContainingPlane();
    if ( plane == nullptr ) {
        return false;
    }
    Vector3Dd planeNormal = plane->getNormal();
    delete plane;
    if ( planeNormal.length() < VSDK::EPSILON ) {
        return false;
    }

    Vector3Dd front = chooseFacingFront(planeNormal, center, camera->getPosition());
    Vector3Dd up = chooseUpVector(camera->getUp(), front);
    Vector3Dd left = up.crossProduct(front).normalized();

    double distance = computeFramingDistance(camera, center, front, left, up,
        faceVertices) * SAFETY_MARGIN;
    if ( distance < VSDK::EPSILON ) {
        distance = java::Math::max(camera->getNearPlaneDistance() * 2.0, 1.0);
    }

    Vector3Dd eye = center.subtract(front.multiply(distance));
    camera->setPosition(eye);
    camera->setUpDirect(up);
    camera->setLeftDirect(left);
    camera->setFocusedPositionDirect(center);

    CameraControllerOrbiter* orbiter =
        dynamic_cast<CameraControllerOrbiter*>(model.getCameraController());
    if ( orbiter != nullptr ) {
        orbiter->setPointOfInterest(center);
    }
    return true;
}

java::ArrayList<_PolyhedralBoundedSolidVertex*>
CameraFaceFocusInteraction::collectFaceVertices(_PolyhedralBoundedSolidFace* face)
{
    java::ArrayList<_PolyhedralBoundedSolidVertex*> vertices;
    // Java: `Set<Integer> visitedIds = new LinkedHashSet<Integer>()`
    java::ArrayList<int> visitedIds;

    long int i;
    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        do {
            _PolyhedralBoundedSolidVertex* v = he->startingVertex;
            if ( v != nullptr && !visitedIds.contains(v->id) ) {
                visitedIds.add(v->id);
                vertices.add(v);
            }
            he = he->next();
        } while ( he != start );
    }
    return vertices;
}

Vector3Dd CameraFaceFocusInteraction::computeCentroid(
    const java::ArrayList<_PolyhedralBoundedSolidVertex*>& vertices)
{
    double sx = 0.0;
    double sy = 0.0;
    double sz = 0.0;

    long int i;
    for ( i = 0; i < vertices.size(); i++ ) {
        Vector3Dd p = vertices.get(i)->position;
        sx += p.x();
        sy += p.y();
        sz += p.z();
    }
    double invN = 1.0 / vertices.size();
    return Vector3Dd(sx * invN, sy * invN, sz * invN);
}

Vector3Dd CameraFaceFocusInteraction::chooseFacingFront(const Vector3Dd& normal,
    const Vector3Dd& center,
    const Vector3Dd& cameraPosition)
{
    Vector3Dd toCamera = cameraPosition.subtract(center);
    if ( normal.dotProduct(toCamera) >= 0.0 ) {
        return normal.multiply(-1).normalized();
    }
    return normal.normalized();
}

Vector3Dd CameraFaceFocusInteraction::chooseUpVector(const Vector3Dd& upHint,
    const Vector3Dd& front)
{
    Vector3Dd projected = upHint.subtract(front.multiply(upHint.dotProduct(front)));
    if ( projected.length() > VSDK::EPSILON ) {
        return projected.normalized();
    }

    Vector3Dd alt1(0, 0, 1);
    projected = alt1.subtract(front.multiply(alt1.dotProduct(front)));
    if ( projected.length() > VSDK::EPSILON ) {
        return projected.normalized();
    }

    Vector3Dd alt2(0, 1, 0);
    projected = alt2.subtract(front.multiply(alt2.dotProduct(front)));
    return projected.normalized();
}

double CameraFaceFocusInteraction::computeFramingDistance(Camera* camera,
    const Vector3Dd& center,
    const Vector3Dd& front,
    const Vector3Dd& left,
    const Vector3Dd& up,
    const java::ArrayList<_PolyhedralBoundedSolidVertex*>& vertices)
{
    if ( camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        return java::Math::max(camera->getPosition().subtract(center).length(),
            camera->getNearPlaneDistance() * 2.0);
    }

    double viewportY = java::Math::max(camera->getViewportYSize(), 1e-9);
    double aspect = camera->getViewportXSize() / viewportY;
    double halfVerticalFov = java::Math::toRadians(camera->getFov() * 0.5);
    double tanVertical = std::tan(halfVerticalFov);
    double halfHorizontalFov = std::atan(java::Math::max(aspect, 1e-9) * tanVertical);
    double tanHorizontal = std::tan(halfHorizontalFov);

    Vector3Dd right = left.multiply(-1);
    double requiredDistance = 0.0;

    long int i;
    for ( i = 0; i < vertices.size(); i++ ) {
        Vector3Dd rel = vertices.get(i)->position.subtract(center);
        double x = rel.dotProduct(right);
        double y = rel.dotProduct(up);
        double z = rel.dotProduct(front);

        double byHorizontal = std::fabs(x) / java::Math::max(tanHorizontal, 1e-9) - z;
        double byVertical = std::fabs(y) / java::Math::max(tanVertical, 1e-9) - z;
        double byNear = camera->getNearPlaneDistance() - z + 1e-6;

        requiredDistance = java::Math::max(requiredDistance, byHorizontal);
        requiredDistance = java::Math::max(requiredDistance, byVertical);
        requiredDistance = java::Math::max(requiredDistance, byNear);
    }

    return java::Math::max(requiredDistance, camera->getNearPlaneDistance() * 2.0);
}
