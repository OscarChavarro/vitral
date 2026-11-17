#include <stddef.h>
#include <math.h>

#include "java/lang/Math.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmoAnimation.h"

const double CameraControlGizmoAnimation::DEFAULT_DURATION_IN_SECONDS = 0.45;

CameraControlGizmoAnimation::CameraControlGizmoAnimation(Camera* camera,
    const Matrix4x4d& targetRotation, double durationInSeconds)
{
    this->camera = camera;
    this->pivot = camera->getFocusedPosition();
    this->distance = camera->getFocusedPosition().subtract(
        camera->getPosition()).length();
    this->startRotation = camera->getRotation().withoutTranslation();
    this->targetRotation = Matrix4x4d(targetRotation).withoutTranslation();
    long long duration = (long long)(durationInSeconds * 1.0e9);
    this->durationInNanoseconds = duration > 0 ? duration : 0;

    double axisAngle[4];
    extractAxisAngle(
        this->targetRotation.multiply(startRotation.transpose()), axisAngle);
    this->axis = Vector3Dd(axisAngle[0], axisAngle[1], axisAngle[2]);
    this->angle = axisAngle[3];
    this->startTime = 0;
    this->started = FALSE;
    this->running = TRUE;
}

BOOLEAN CameraControlGizmoAnimation::isRunning() const
{
    return running;
}

Camera* CameraControlGizmoAnimation::getCamera() const
{
    return camera;
}

double CameraControlGizmoAnimation::getAngle() const
{
    return angle;
}

BOOLEAN CameraControlGizmoAnimation::advance(long long nanoTime)
{
    if ( !running ) {
        return FALSE;
    }
    if ( !started ) {
        started = TRUE;
        startTime = nanoTime;
    }
    double s = 1.0;
    if ( durationInNanoseconds > 0 ) {
        s = (double)(nanoTime - startTime) / (double)durationInNanoseconds;
    }
    if ( s >= 1.0 ) {
        finish();
        return TRUE;
    }
    applyFraction(easeInOut(Math::max(0.0, s)));
    return TRUE;
}

void CameraControlGizmoAnimation::finish()
{
    running = FALSE;
    applyRotation(targetRotation);
}

void CameraControlGizmoAnimation::cancel()
{
    running = FALSE;
}

double CameraControlGizmoAnimation::easeInOut(double s)
{
    return s * s * (3 - 2 * s);
}

void CameraControlGizmoAnimation::applyFraction(double fraction)
{
    if ( angle == 0.0 ) {
        applyRotation(targetRotation);
        return;
    }
    Matrix4x4d partial = Matrix4x4d().axisRotation(angle * fraction, axis);
    applyRotation(partial.multiply(startRotation));
}

void CameraControlGizmoAnimation::applyRotation(const Matrix4x4d& rotation)
{
    Vector3Dd front = Vector3Dd(rotation.get(0, 0), rotation.get(1, 0),
        rotation.get(2, 0)).normalized();

    camera->setRotation(rotation);
    camera->setPosition(pivot.subtract(front.multiply(distance)));
    camera->updateVectors();
}

void CameraControlGizmoAnimation::extractAxisAngle(const Matrix4x4d& r,
    double out[4])
{
    double trace = r.get(0, 0) + r.get(1, 1) + r.get(2, 2);
    double cosine = Math::max(-1.0, Math::min(1.0, (trace - 1.0) / 2.0));
    double angle = Math::acos(cosine);
    double x = r.get(2, 1) - r.get(1, 2);
    double y = r.get(0, 2) - r.get(2, 0);
    double z = r.get(1, 0) - r.get(0, 1);
    double length = Math::sqrt(x * x + y * y + z * z);

    if ( angle < 1e-9 ) {
        out[0] = 0;
        out[1] = 0;
        out[2] = 1;
        out[3] = 0;
        return;
    }
    if ( length > 1e-6 ) {
        out[0] = x / length;
        out[1] = y / length;
        out[2] = z / length;
        out[3] = angle;
        return;
    }

    // Half turn: R = 2 a a^T - I, the axis comes from the diagonal
    double xx = Math::sqrt(Math::max(0.0, (r.get(0, 0) + 1) / 2));
    double yy = Math::sqrt(Math::max(0.0, (r.get(1, 1) + 1) / 2));
    double zz = Math::sqrt(Math::max(0.0, (r.get(2, 2) + 1) / 2));
    if ( xx >= yy && xx >= zz ) {
        yy = (r.get(0, 1) + r.get(1, 0)) / (4 * xx);
        zz = (r.get(0, 2) + r.get(2, 0)) / (4 * xx);
    }
    else if ( yy >= zz ) {
        xx = (r.get(0, 1) + r.get(1, 0)) / (4 * yy);
        zz = (r.get(1, 2) + r.get(2, 1)) / (4 * yy);
    }
    else {
        xx = (r.get(0, 2) + r.get(2, 0)) / (4 * zz);
        yy = (r.get(1, 2) + r.get(2, 1)) / (4 * zz);
    }
    double l = Math::sqrt(xx * xx + yy * yy + zz * zz);
    out[0] = xx / l;
    out[1] = yy / l;
    out[2] = zz / l;
    out[3] = Math::PI;
}
