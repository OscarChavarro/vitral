#include <cmath>

#include "vsdk/toolkit/environment/camera/Camera.h"
#include "model/history/CameraState.h"

namespace {
/// Relative tolerance for vectors to be the same
const double VECTOR_TOLERANCE = 1.0e-9;
}

CameraState::CameraState(Camera* camera)
    : camera(camera), position(camera->getPosition()), up(camera->getUp()),
      front(camera->getFront()), left(camera->getLeft()),
      focusedPosition(camera->getFocusedPosition()),
      projectionMode(camera->getProjectionMode()), fov(camera->getFov()),
      orthogonalZoom(camera->getOrthogonalZoom()),
      nearPlaneDistance(camera->getNearPlaneDistance()),
      farPlaneDistance(camera->getFarPlaneDistance())
{
}

CameraState* CameraState::capture(Camera* camera)
{
    return new CameraState(camera);
}

Entity* CameraState::getEntity() const
{
    return camera;
}

void CameraState::restore() const
{
    camera->setPosition(position);
    // Gives back the front direction and the focal distance...
    camera->setFocusedPositionDirect(focusedPosition);
    // ... and the exact frame, which is not recomputed from the former
    camera->setUpDirect(up);
    camera->setLeftDirect(left);
    camera->setProjectionMode(projectionMode);
    camera->setFov(fov);
    camera->setOrthogonalZoom(orthogonalZoom);
    camera->setNearPlaneDistance(nearPlaneDistance);
    camera->setFarPlaneDistance(farPlaneDistance);
    camera->updateVectors();
}

bool CameraState::isSameState(const EntityTransformState* other) const
{
    const CameraState* state = dynamic_cast<const CameraState*>(other);
    if ( state == nullptr ) {
        return false;
    }
    return state->camera == camera &&
        sameVector(state->position, position) &&
        sameVector(state->up, up) &&
        sameVector(state->front, front) &&
        sameVector(state->left, left) &&
        sameVector(state->focusedPosition, focusedPosition) &&
        state->projectionMode == projectionMode &&
        state->fov == fov &&
        state->orthogonalZoom == orthogonalZoom &&
        state->nearPlaneDistance == nearPlaneDistance &&
        state->farPlaneDistance == farPlaneDistance;
}

EntityTransformState* CameraState::clone() const
{
    return new CameraState(*this);
}

bool CameraState::sameVector(const Vector3Dd& a, const Vector3Dd& b)
{
    double scale = std::fmax(1.0, std::fmax(a.length(), b.length()));

    return a.subtract(b).length() <= VECTOR_TOLERANCE * scale;
}
