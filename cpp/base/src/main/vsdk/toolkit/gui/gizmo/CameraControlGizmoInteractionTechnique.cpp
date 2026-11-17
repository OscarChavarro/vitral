#include <cmath>

#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmoAnimation.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmoInteractionTechnique.h"

const int CameraControlGizmoInteractionTechnique::DRAG_THRESHOLD_IN_PIXELS;
const double CameraControlGizmoInteractionTechnique::ORBIT_RADIANS_PER_PIXEL =
    java::Math::toRadians(0.6);
const double CameraControlGizmoInteractionTechnique::MAX_ELEVATION =
    java::Math::toRadians(89.0);

CameraControlGizmoInteractionTechnique::CameraControlGizmoInteractionTechnique(
    CameraControlGizmo* gizmo)
{
    this->gizmo = gizmo;
    animationDurationInSeconds =
        CameraControlGizmoAnimation::DEFAULT_DURATION_IN_SECONDS;
    animation = nullptr;
    pressHudCamera = nullptr;
    endGesture();
}

CameraControlGizmoInteractionTechnique::~CameraControlGizmoInteractionTechnique()
{
    delete animation;
    delete pressHudCamera;
}

CameraControlGizmo*
CameraControlGizmoInteractionTechnique::getGizmo() const
{
    return gizmo;
}

double
CameraControlGizmoInteractionTechnique::getAnimationDurationInSeconds() const
{
    return animationDurationInSeconds;
}

void
CameraControlGizmoInteractionTechnique::setAnimationDurationInSeconds(
    double seconds)
{
    animationDurationInSeconds = seconds;
}

bool
CameraControlGizmoInteractionTechnique::isActive() const
{
    return gizmo->isActive() || gestureInCourse;
}

bool
CameraControlGizmoInteractionTechnique::isGestureInCourse() const
{
    return gestureInCourse;
}

bool
CameraControlGizmoInteractionTechnique::isAnimating() const
{
    return animation != nullptr && animation->isRunning();
}

bool
CameraControlGizmoInteractionTechnique::advanceAnimation(long long nanoTime)
{
    if ( !isAnimating() ) {
        return false;
    }
    bool changed = animation->advance(nanoTime);
    gizmo->updateHudCamera();
    return changed;
}

void
CameraControlGizmoInteractionTechnique::cancelAnimation()
{
    if ( animation != nullptr ) {
        animation->cancel();
        delete animation;
        animation = nullptr;
    }
}

bool
CameraControlGizmoInteractionTechnique::animateToRegion(int region)
{
    Camera* camera = gizmo->getCamera();

    if ( camera == nullptr || region < 0 ||
         region >= CameraControlGizmo::NUMBER_OF_CUBE_REGIONS ) {
        return false;
    }
    cancelAnimation();
    Matrix4x4d target;
    if ( !gizmo->calculateViewRotation(region, camera->getRotation(),
             &target) ) {
        return false;
    }
    animation = new CameraControlGizmoAnimation(camera, target,
        animationDurationInSeconds);
    return true;
}

//= Mouse =================================================================

bool
CameraControlGizmoInteractionTechnique::processMouseMovedEvent(
    const MouseEvent& e)
{
    if ( gestureInCourse ) {
        return false;
    }
    return updateHover(e.getX(), e.getY());
}

bool
CameraControlGizmoInteractionTechnique::processMouseExitedEvent()
{
    if ( gestureInCourse ) {
        return false;
    }
    bool changed = gizmo->isActive() ||
        gizmo->getVolatileSelection() != CameraControlGizmo::NO_REGION;
    gizmo->setActive(false);
    gizmo->setVolatileSelection(CameraControlGizmo::NO_REGION);
    return changed;
}

bool
CameraControlGizmoInteractionTechnique::processMousePressedEvent(
    const MouseEvent& e)
{
    endGesture();
    if ( !gizmo->containsPixel(e.getX(), e.getY()) ||
         gizmo->getCamera() == nullptr ) {
        return false;
    }
    updateHover(e.getX(), e.getY());
    cancelAnimation();

    Camera* camera = gizmo->getCamera();
    gestureInCourse = true;
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
    return true;
}

bool
CameraControlGizmoInteractionTechnique::processMouseDraggedEvent(
    const MouseEvent& e)
{
    if ( !gestureInCourse ) {
        return false;
    }
    int dx = e.getX() - pressX;
    int dy = e.getY() - pressY;

    if ( !dragging ) {
        if ( java::Math::abs(dx) < DRAG_THRESHOLD_IN_PIXELS &&
             java::Math::abs(dy) < DRAG_THRESHOLD_IN_PIXELS ) {
            return false;
        }
        dragging = true;
    }

    Matrix4x4d rotation;
    bool withRotation = false;
    if ( pressRegion == CameraControlGizmo::COMPASS_REGION ) {
        withRotation = compassRotation(e.getX(), e.getY(), dx, &rotation);
    }
    if ( !withRotation ) {
        withRotation = orbitRotation(dx, dy, &rotation);
    }
    if ( !withRotation ) {
        return false;
    }
    applyRotation(rotation);
    return true;
}

bool
CameraControlGizmoInteractionTechnique::processMouseReleasedEvent(
    const MouseEvent& e)
{
    if ( !gestureInCourse ) {
        return false;
    }
    bool wasDragging = dragging;
    int region = pressRegion;

    endGesture();
    if ( !wasDragging && region >= 0 &&
         region < CameraControlGizmo::NUMBER_OF_CUBE_REGIONS ) {
        animateToRegion(region);
    }
    updateHover(e.getX(), e.getY());
    return true;
}

//= Gesture computations ==================================================

bool
CameraControlGizmoInteractionTechnique::orbitRotation(int dx, int dy,
    Matrix4x4d* result)
{
    double scale = ORBIT_RADIANS_PER_PIXEL *
        ((double)gizmo->getBaseSizeInPixels() /
         java::Math::max(1, gizmo->getSizeInPixels()));
    Vector3Dd front = Vector3Dd(pressRotation.get(0, 0),
        pressRotation.get(1, 0), pressRotation.get(2, 0)).normalized();
    double elevation = java::Math::asin(java::Math::max(-1.0,
        java::Math::min(1.0, front.z())));
    double azimuth = std::atan2(front.y(), front.x());

    // Dragging moves the cube with the cursor: right turns the scene
    // counterclockwise (seen from above), down shows more of its top
    azimuth -= dx * scale;
    elevation -= dy * scale;
    elevation = java::Math::max(-MAX_ELEVATION,
        java::Math::min(MAX_ELEVATION, elevation));

    Vector3Dd newFront(
        java::Math::cos(elevation) * java::Math::cos(azimuth),
        java::Math::cos(elevation) * java::Math::sin(azimuth),
        java::Math::sin(elevation));
    return CameraControlGizmo::createCameraRotation(newFront,
        Vector3Dd(0, 0, 1), result);
}

bool
CameraControlGizmoInteractionTechnique::compassRotation(int x, int y,
    int dx, Matrix4x4d* result)
{
    (void)dx;
    if ( java::Double::isNaN(pressCompassAngle) ) {
        return false;
    }
    double angle = CameraControlGizmo::compassAngleAt(x - gizmo->getHudX(),
        y - gizmo->getHudY(), *pressHudCamera);
    if ( java::Double::isNaN(angle) ) {
        return false;
    }
    double sweep = angle - pressCompassAngle;
    // The scene turns by the sweep, so the camera turns the other way
    *result = Matrix4x4d().axisRotation(-sweep, Vector3Dd(0, 0, 1))
        .multiply(pressRotation);
    return true;
}

void
CameraControlGizmoInteractionTechnique::applyRotation(
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

bool
CameraControlGizmoInteractionTechnique::updateHover(int x, int y)
{
    bool wasActive = gizmo->isActive();
    int previous = gizmo->getVolatileSelection();
    bool inside = gizmo->containsPixel(x, y);

    gizmo->setActive(inside);
    gizmo->setVolatileSelection(inside ? gizmo->pickRegion(x, y) :
        CameraControlGizmo::NO_REGION);
    return wasActive != gizmo->isActive() ||
        previous != gizmo->getVolatileSelection();
}

void
CameraControlGizmoInteractionTechnique::endGesture()
{
    gestureInCourse = false;
    dragging = false;
    pressRegion = CameraControlGizmo::NO_REGION;
    pressX = 0;
    pressY = 0;
    delete pressHudCamera;
    pressHudCamera = nullptr;
    pressRotation = Matrix4x4d();
    pressPivot = Vector3Dd();
    pressDistance = 0;
    pressCompassAngle = java::Double::NaN;
}
