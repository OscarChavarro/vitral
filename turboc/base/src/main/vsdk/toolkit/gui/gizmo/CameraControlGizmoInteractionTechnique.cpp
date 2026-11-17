#include <stddef.h>
#include <stdlib.h>
#include <math.h>

#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmoAnimation.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmoInteractionTechnique.h"

const double CameraControlGizmoInteractionTechnique::ORBIT_RADIANS_PER_PIXEL =
    Math::toRadians(0.6);
const double CameraControlGizmoInteractionTechnique::MAX_ELEVATION =
    Math::toRadians(89.0);

CameraControlGizmoInteractionTechnique::CameraControlGizmoInteractionTechnique(
    CameraControlGizmo* gizmo)
    : gizmo(gizmo), pressHudCamera(NULL)
{
    animationDurationInSeconds =
        CameraControlGizmoAnimation::DEFAULT_DURATION_IN_SECONDS;
    animation = NULL;
    endGesture();
}

CameraControlGizmoInteractionTechnique::~CameraControlGizmoInteractionTechnique()
{
    delete animation;
    delete pressHudCamera;
}

CameraControlGizmo* CameraControlGizmoInteractionTechnique::getGizmo() const
{
    return gizmo;
}

double
CameraControlGizmoInteractionTechnique::getAnimationDurationInSeconds() const
{
    return animationDurationInSeconds;
}

void CameraControlGizmoInteractionTechnique::setAnimationDurationInSeconds(
    double seconds)
{
    animationDurationInSeconds = seconds;
}

BOOLEAN CameraControlGizmoInteractionTechnique::isActive() const
{
    return gizmo->isActive() || gestureInCourse;
}

BOOLEAN CameraControlGizmoInteractionTechnique::isGestureInCourse() const
{
    return gestureInCourse;
}

BOOLEAN CameraControlGizmoInteractionTechnique::isAnimating() const
{
    return animation != NULL && animation->isRunning();
}

BOOLEAN CameraControlGizmoInteractionTechnique::advanceAnimation(
    long long nanoTime)
{
    if ( !isAnimating() ) {
        return FALSE;
    }
    BOOLEAN changed = animation->advance(nanoTime);
    gizmo->updateHudCamera();
    return changed;
}

void CameraControlGizmoInteractionTechnique::cancelAnimation()
{
    if ( animation != NULL ) {
        animation->cancel();
        delete animation;
        animation = NULL;
    }
}

BOOLEAN CameraControlGizmoInteractionTechnique::animateToRegion(int region)
{
    Camera* camera = gizmo->getCamera();

    if ( camera == NULL || region < 0 ||
         region >= CameraControlGizmo::NUMBER_OF_CUBE_REGIONS ) {
        return FALSE;
    }
    cancelAnimation();
    Matrix4x4d target;
    if ( !gizmo->calculateViewRotation(region, camera->getRotation(),
             target) ) {
        return FALSE;
    }
    animation = new CameraControlGizmoAnimation(camera, target,
        animationDurationInSeconds);
    return TRUE;
}

//= Mouse =================================================================

BOOLEAN CameraControlGizmoInteractionTechnique::processMouseMovedEvent(
    const MouseEvent& e)
{
    if ( gestureInCourse ) {
        return FALSE;
    }
    return updateHover(e.getX(), e.getY());
}

BOOLEAN CameraControlGizmoInteractionTechnique::processMouseExitedEvent()
{
    if ( gestureInCourse ) {
        return FALSE;
    }
    BOOLEAN changed = gizmo->isActive() ||
        gizmo->getVolatileSelection() != CameraControlGizmo::NO_REGION;
    gizmo->setActive(FALSE);
    gizmo->setVolatileSelection(CameraControlGizmo::NO_REGION);
    return changed;
}

BOOLEAN CameraControlGizmoInteractionTechnique::processMousePressedEvent(
    const MouseEvent& e)
{
    endGesture();
    if ( !gizmo->containsPixel(e.getX(), e.getY()) ||
         gizmo->getCamera() == NULL ) {
        return FALSE;
    }
    updateHover(e.getX(), e.getY());
    cancelAnimation();

    Camera* camera = gizmo->getCamera();
    gestureInCourse = TRUE;
    pressRegion = gizmo->pickRegion(e.getX(), e.getY());
    pressX = e.getX();
    pressY = e.getY();
    pressHudCamera = new Camera(*gizmo->getHudCamera());
    pressRotation = camera->getRotation().withoutTranslation();
    pressPivot = camera->getFocusedPosition();
    pressDistance = pressPivot.subtract(camera->getPosition()).length();
    pressCompassAngle = CameraControlGizmo::compassAngleAt(
        e.getX() - gizmo->getHudX(), e.getY() - gizmo->getHudY(),
        *pressHudCamera);
    return TRUE;
}

BOOLEAN CameraControlGizmoInteractionTechnique::processMouseDraggedEvent(
    const MouseEvent& e)
{
    if ( !gestureInCourse ) {
        return FALSE;
    }
    int dx = e.getX() - pressX;
    int dy = e.getY() - pressY;

    if ( !dragging ) {
        if ( abs(dx) < DRAG_THRESHOLD_IN_PIXELS &&
             abs(dy) < DRAG_THRESHOLD_IN_PIXELS ) {
            return FALSE;
        }
        dragging = TRUE;
    }

    Matrix4x4d rotation;
    BOOLEAN hasRotation = FALSE;
    if ( pressRegion == CameraControlGizmo::COMPASS_REGION ) {
        hasRotation = compassRotation(e.getX(), e.getY(), dx, rotation);
    }
    if ( !hasRotation ) {
        hasRotation = orbitRotation(dx, dy, rotation);
    }
    if ( !hasRotation ) {
        return FALSE;
    }
    applyRotation(rotation);
    return TRUE;
}

BOOLEAN CameraControlGizmoInteractionTechnique::processMouseReleasedEvent(
    const MouseEvent& e)
{
    if ( !gestureInCourse ) {
        return FALSE;
    }
    BOOLEAN wasDragging = dragging;
    int region = pressRegion;

    endGesture();
    if ( !wasDragging && region >= 0 &&
         region < CameraControlGizmo::NUMBER_OF_CUBE_REGIONS ) {
        animateToRegion(region);
    }
    updateHover(e.getX(), e.getY());
    return TRUE;
}

//= Gesture computations ==================================================

BOOLEAN CameraControlGizmoInteractionTechnique::orbitRotation(int dx, int dy,
    Matrix4x4d& out)
{
    double scale = ORBIT_RADIANS_PER_PIXEL *
        ((double)gizmo->getBaseSizeInPixels() /
        Math::max(1, gizmo->getSizeInPixels()));
    Vector3Dd front = Vector3Dd(pressRotation.get(0, 0),
        pressRotation.get(1, 0), pressRotation.get(2, 0)).normalized();
    double elevation = Math::asin(Math::max(-1.0, Math::min(1.0, front.z())));
    double azimuth = atan2(front.y(), front.x());

    // Dragging moves the cube with the cursor: right turns the scene
    // counterclockwise (seen from above), down shows more of its top
    azimuth -= dx * scale;
    elevation -= dy * scale;
    elevation = Math::max(-MAX_ELEVATION, Math::min(MAX_ELEVATION, elevation));

    Vector3Dd newFront(
        Math::cos(elevation) * Math::cos(azimuth),
        Math::cos(elevation) * Math::sin(azimuth),
        Math::sin(elevation));
    return CameraControlGizmo::createCameraRotation(newFront,
        Vector3Dd(0, 0, 1), out);
}

BOOLEAN CameraControlGizmoInteractionTechnique::compassRotation(int x, int y,
    int dx, Matrix4x4d& out)
{
    (void)dx;
    if ( Double::isNaN(pressCompassAngle) ) {
        return FALSE;
    }
    double angle = CameraControlGizmo::compassAngleAt(x - gizmo->getHudX(),
        y - gizmo->getHudY(), *pressHudCamera);
    if ( Double::isNaN(angle) ) {
        return FALSE;
    }
    double sweep = angle - pressCompassAngle;
    // The scene turns by the sweep, so the camera turns the other way
    out = Matrix4x4d().axisRotation(-sweep, Vector3Dd(0, 0, 1))
        .multiply(pressRotation);
    return TRUE;
}

void CameraControlGizmoInteractionTechnique::applyRotation(
    const Matrix4x4d& rotation)
{
    Camera* camera = gizmo->getCamera();
    Vector3Dd front = Vector3Dd(rotation.get(0, 0), rotation.get(1, 0),
        rotation.get(2, 0)).normalized();

    camera->setRotation(rotation);
    camera->setPosition(pressPivot.subtract(front.multiply(pressDistance)));
    camera->updateVectors();
    gizmo->updateHudCamera();
}

BOOLEAN CameraControlGizmoInteractionTechnique::updateHover(int x, int y)
{
    BOOLEAN wasActive = gizmo->isActive();
    int previous = gizmo->getVolatileSelection();
    BOOLEAN inside = gizmo->containsPixel(x, y);

    gizmo->setActive(inside);
    gizmo->setVolatileSelection(inside ? gizmo->pickRegion(x, y) :
        (int)CameraControlGizmo::NO_REGION);
    return wasActive != gizmo->isActive() ||
        previous != gizmo->getVolatileSelection();
}

void CameraControlGizmoInteractionTechnique::endGesture()
{
    gestureInCourse = FALSE;
    dragging = FALSE;
    pressRegion = CameraControlGizmo::NO_REGION;
    pressX = 0;
    pressY = 0;
    delete pressHudCamera;
    pressHudCamera = NULL;
    pressRotation = Matrix4x4d();
    pressPivot = Vector3Dd(0, 0, 0);
    pressDistance = 0;
    pressCompassAngle = Double::NaN;
}
