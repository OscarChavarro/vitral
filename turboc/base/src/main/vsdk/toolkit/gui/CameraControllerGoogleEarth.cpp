#include <math.h>
#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/CameraControllerGoogleEarth.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#ifdef PI
#undef PI
#endif
static const double PI = 3.14159265358979323846;

CameraControllerGoogleEarth::CameraControllerGoogleEarth(Camera* camera)
    : camera(camera), jumpStep(1e-17), xOld(0), yOld(0)
{
}

BOOLEAN CameraControllerGoogleEarth::processMousePressedEvent(const MouseEvent& e)
{
    xOld = e.getX();
    yOld = e.getY();
    camera->updateVectors();
    Ray rayA = camera->generateRay(xOld, yOld);
    (void)rayA;
    return FALSE;
}

BOOLEAN CameraControllerGoogleEarth::processMouseReleasedEvent(const MouseEvent&)
{
    return FALSE;
}

BOOLEAN CameraControllerGoogleEarth::processMouseClickedEvent(const MouseEvent&)
{
    return FALSE;
}

BOOLEAN CameraControllerGoogleEarth::processMouseMovedEvent(const MouseEvent&)
{
    return FALSE;
}

BOOLEAN CameraControllerGoogleEarth::processMouseDraggedEvent(const MouseEvent& e)
{
    int prevX = xOld;
    int prevY = yOld;
    int x = e.getX();
    int y = e.getY();

    Ray rayA = camera->generateRay(prevX, prevY);
    Ray rayB = camera->generateRay(x, y);
    InfinitePlane infinitePlane(Vector3Dd(0, 0, 1), Vector3Dd(0, 0, 0));
    Ray* hitA = infinitePlane.doIntersectionFirstHit(rayA);
    Ray* hitB = infinitePlane.doIntersectionFirstHit(rayB);
    if ( hitA == NULL || hitB == NULL ) {
        delete hitA;
        delete hitB;
        return FALSE;
    }

    Vector3Dd pA(
        hitA->getOrigin().x() + hitA->getDirection().x() * hitA->getT(),
        hitA->getOrigin().y() + hitA->getDirection().y() * hitA->getT(),
        hitA->getOrigin().z() + hitA->getDirection().z() * hitA->getT());
    Vector3Dd pB(
        hitB->getOrigin().x() + hitB->getDirection().x() * hitB->getT(),
        hitB->getOrigin().y() + hitB->getDirection().y() * hitB->getT(),
        hitB->getOrigin().z() + hitB->getDirection().z() * hitB->getT());
    Vector3Dd d = pB.subtract(pA);

    Vector3Dd currentPosition = camera->getPosition();
    camera->setPosition(Vector3Dd(
        currentPosition.x() - d.x(),
        currentPosition.y() - d.y(),
        currentPosition.z()));

    xOld = x;
    yOld = y;
    delete hitA;
    delete hitB;

    return TRUE;
}

BOOLEAN CameraControllerGoogleEarth::processMouseWheelEvent(const MouseEvent& e)
{
    Vector3Dd eyePosition = camera->getPosition();
    Vector3Dd focusedPosition = camera->getFocusedPosition();
    Matrix4x4d R = camera->getRotation();
    int projectionMode = camera->getProjectionMode();
    double fov = camera->getFov();
    double orthogonalZoom = camera->getOrthogonalZoom();
    double nearPlaneDistance = camera->getNearPlaneDistance();
    double farPlaneDistance = camera->getFarPlaneDistance();

    int clicks = e.getClicks();
    BOOLEAN updated = FALSE;

    if ( clicks < 0 ) {
        double expo = Cpp98Support::round(log10(Math::max(eyePosition.z(), 1e-9))) - 1;
        jumpStep = pow(10, expo);

        if ( (eyePosition.z() - jumpStep) <= 12 ) {
            return FALSE;
        }

        double h = eyePosition.z();
        nearPlaneDistance = h * 0.1;
        farPlaneDistance = h * 110;

        eyePosition = eyePosition.withZ(eyePosition.z() - jumpStep);
        focusedPosition = focusedPosition.withZ(focusedPosition.z() - jumpStep);
        updated = TRUE;
    }
    else if ( clicks > 0 ) {
        if ( (eyePosition.z() + jumpStep) >= pow(10.0, 24.0) ) {
            return FALSE;
        }

        jumpStep = pow(10, Cpp98Support::round(log10(Math::max(eyePosition.z(), 1e-9))) - 1);

        double h = eyePosition.z();
        nearPlaneDistance = h * 0.1;
        farPlaneDistance = h * 110;

        eyePosition = eyePosition.withZ(eyePosition.z() + jumpStep);
        focusedPosition = focusedPosition.withZ(focusedPosition.z() + jumpStep);
        updated = TRUE;
    }

    camera->setPosition(eyePosition);
    camera->setFocusedPositionMaintainingOrthogonality(focusedPosition);
    camera->setRotation(R);
    camera->setOrthogonalZoom(orthogonalZoom);
    camera->setFov(fov);
    camera->setProjectionMode(projectionMode);
    camera->setNearPlaneDistance(nearPlaneDistance);
    camera->setFarPlaneDistance(farPlaneDistance);

    return updated;
}

Camera* CameraControllerGoogleEarth::getCamera()
{
    return camera;
}

void CameraControllerGoogleEarth::setCamera(Camera* camera)
{
    this->camera = camera;
}

BOOLEAN CameraControllerGoogleEarth::processKeyPressedEvent(const KeyEvent& keyEvent)
{
    Vector3Dd eyePosition = camera->getPosition();
    Vector3Dd focusedPosition = camera->getFocusedPosition();
    Matrix4x4d R = camera->getRotation();
    int projectionMode = camera->getProjectionMode();
    double fov = camera->getFov();
    double orthogonalZoom = camera->getOrthogonalZoom();
    double nearPlaneDistance = camera->getNearPlaneDistance();
    double farPlaneDistance = camera->getFarPlaneDistance();

    double yaw = R.obtainEulerYawAngle();
    double pitch = R.obtainEulerPitchAngle();
    double roll = R.obtainEulerRollAngle();

    double angleInc;
    BOOLEAN updated = FALSE;
    double epsilon = 0.0001;

    if (fov > 90) angleInc = PI * 10.0 / 180.0;
    else if (fov > 45) angleInc = PI * 5.0 / 180.0;
    else if (fov > 15) angleInc = PI * 2.5 / 180.0;
    else if (fov > 5) angleInc = PI * 1.0 / 180.0;
    else angleInc = PI * 0.1 / 180.0;

    switch (keyEvent.keycode) {
        case KeyEvent::KEY_UP:
            pitch -= angleInc;
            if (pitch < -PI / 2.0) pitch = -PI / 2.0;
            updated = TRUE;
            break;
        case KeyEvent::KEY_DOWN:
            pitch += angleInc;
            if (pitch > PI / 2.0) pitch = PI / 2.0;
            updated = TRUE;
            break;
        case KeyEvent::KEY_LEFT:
            yaw += angleInc;
            while (yaw >= 2.0 * PI) yaw -= 2.0 * PI;
            updated = TRUE;
            break;
        case KeyEvent::KEY_RIGHT:
            yaw -= angleInc;
            while (yaw < 0) yaw += 2.0 * PI;
            updated = TRUE;
            break;

        case KeyEvent::KEY_x:
            eyePosition = eyePosition.withX(eyePosition.x() - jumpStep);
            focusedPosition = focusedPosition.withX(focusedPosition.x() - jumpStep);
            updated = TRUE;
            break;
        case KeyEvent::KEY_X:
            eyePosition = eyePosition.withX(eyePosition.x() + jumpStep);
            focusedPosition = focusedPosition.withX(focusedPosition.x() + jumpStep);
            updated = TRUE;
            break;
        case KeyEvent::KEY_y:
            eyePosition = eyePosition.withY(eyePosition.y() - jumpStep);
            focusedPosition = focusedPosition.withY(focusedPosition.y() - jumpStep);
            updated = TRUE;
            break;
        case KeyEvent::KEY_Y:
            eyePosition = eyePosition.withY(eyePosition.y() + jumpStep);
            updated = TRUE;
            break;
        case KeyEvent::KEY_z: {
            double expo = Cpp98Support::round(log10(Math::max(eyePosition.z(), 1e-9))) - 1;
            jumpStep = pow(10, expo);

            double h = eyePosition.z();
            if ( (eyePosition.z() - jumpStep) <= 12 ) {
                break;
            }

            nearPlaneDistance = h * 0.1;
            farPlaneDistance = h * 110;

            eyePosition = eyePosition.withZ(eyePosition.z() - jumpStep);
            focusedPosition = focusedPosition.withZ(focusedPosition.z() - jumpStep);
            updated = TRUE;
            break;
        }
        case KeyEvent::KEY_Z: {
            if ( (eyePosition.z() + jumpStep) >= pow(10.0, 4.0) ) {
                break;
            }

            jumpStep = pow(10, Cpp98Support::round(log10(Math::max(eyePosition.z(), 1e-9))) - 1);
            double h = eyePosition.z();
            nearPlaneDistance = h * 0.1;
            farPlaneDistance = h * 110;

            eyePosition = eyePosition.withZ(eyePosition.z() + jumpStep);
            focusedPosition = focusedPosition.withZ(focusedPosition.z() + jumpStep);
            updated = TRUE;
            break;
        }
        case KeyEvent::KEY_S:
            roll -= PI * 5.0 / 180.0;
            while (roll < 0) roll += 2.0 * PI;
            updated = TRUE;
            break;
        case KeyEvent::KEY_s:
            roll += PI * 5.0 / 180.0;
            while (roll > 2.0 * PI) roll -= 2.0 * PI;
            updated = TRUE;
            break;

        case KeyEvent::KEY_A:
            if (camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL) {
                orthogonalZoom /= 2;
            }
            else {
                if (fov < 0.1 - epsilon) fov += 0.1;
                else if (fov < 1 - epsilon) fov++;
                else if (fov < 175 - epsilon) fov += 5;
            }
            updated = TRUE;
            break;
        case KeyEvent::KEY_a:
            if (camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL) {
                orthogonalZoom *= 2;
            }
            else {
                if (fov > 5 + epsilon) fov -= 5;
                else if (fov > 1 + epsilon) fov--;
                else if (fov > 0.1 + epsilon) fov -= 0.1;
            }
            updated = TRUE;
            break;

        case KeyEvent::KEY_N:
            nearPlaneDistance += 0.5;
            updated = TRUE;
            break;
        case KeyEvent::KEY_n:
            nearPlaneDistance -= 0.5;
            updated = TRUE;
            break;

        case KeyEvent::KEY_F:
            farPlaneDistance += 0.5;
            updated = TRUE;
            break;
        case KeyEvent::KEY_f:
            farPlaneDistance -= 0.5;
            updated = TRUE;
            break;

        case KeyEvent::KEY_p:
            projectionMode = (projectionMode == Camera::PROJECTION_MODE_PERSPECTIVE)
                ? Camera::PROJECTION_MODE_ORTHOGONAL
                : Camera::PROJECTION_MODE_PERSPECTIVE;
            updated = TRUE;
            break;

        case KeyEvent::KEY_i:
            fprintf(stdout, "%s\n", camera->toString().c_str());
            break;
    }

    R = R.eulerAnglesRotation(yaw, pitch, roll);

    camera->setPosition(eyePosition);
    camera->setFocusedPositionMaintainingOrthogonality(focusedPosition);
    camera->setRotation(R);
    camera->setOrthogonalZoom(orthogonalZoom);
    camera->setFov(fov);
    camera->setProjectionMode(projectionMode);
    camera->setNearPlaneDistance(nearPlaneDistance);
    camera->setFarPlaneDistance(farPlaneDistance);

    return updated;
}

BOOLEAN CameraControllerGoogleEarth::processKeyReleasedEvent(const KeyEvent&)
{
    return FALSE;
}

void CameraControllerGoogleEarth::setDeltaMovement(double factor)
{
    if ( factor > 0 ) {
        jumpStep = factor;
    }
}

void CameraControllerGoogleEarth::zoomOut(double jumpValue)
{
    Vector3Dd eyePosition = camera->getPosition();
    Vector3Dd focusedPosition = camera->getFocusedPosition();

    double h = eyePosition.z();
    double nearPlaneDistance = h * 0.1;
    double farPlaneDistance = h * 110;

    eyePosition = eyePosition.withZ(eyePosition.z() + jumpValue);
    focusedPosition = focusedPosition.withZ(focusedPosition.z() + jumpValue);

    camera->setPosition(eyePosition);
    camera->setFocusedPositionMaintainingOrthogonality(focusedPosition);
    camera->setNearPlaneDistance(nearPlaneDistance);
    camera->setFarPlaneDistance(farPlaneDistance);
}

void CameraControllerGoogleEarth::zoomIn(double jumpValue)
{
    Vector3Dd eyePosition = camera->getPosition();
    Vector3Dd focusedPosition = camera->getFocusedPosition();

    double h = eyePosition.z();
    if ( (eyePosition.z() - jumpValue) >= 12 ) {
        double nearPlaneDistance = h * 0.1;
        double farPlaneDistance = h * 110;

        eyePosition = eyePosition.withZ(eyePosition.z() - jumpValue);
        focusedPosition = focusedPosition.withZ(focusedPosition.z() - jumpValue);

        camera->setPosition(eyePosition);
        camera->setFocusedPositionMaintainingOrthogonality(focusedPosition);
        camera->setNearPlaneDistance(nearPlaneDistance);
        camera->setFarPlaneDistance(farPlaneDistance);
    }
}

const Class CameraControllerGoogleEarth::CLASS = { "CameraControllerGoogleEarth", &CameraController::CLASS };

const Class *
CameraControllerGoogleEarth::getClass() const
{
    return &CLASS;
}
