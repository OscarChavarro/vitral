#ifndef __CAMERA_STATE__
#define __CAMERA_STATE__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "model/history/EntityTransformState.h"

class Camera;

/**
Placement and projection of a camera: eye position, reference frame (up,
front, left), focused point, projection mode, field of view, orthogonal zoom
and clipping planes. It is restored through the setters of `Camera`, so
whoever caches data derived from the camera sees it modified. The size of the
viewport the camera projects to is not part of the state: it belongs to the
viewport.

Vectors are compared with a tolerance: `Camera::updateVectors`, called each
time a camera is drawn, normalizes its frame again, which changes the last
bits of vectors nobody moved.
*/
class CameraState : public EntityTransformState {
private:
    Camera* camera;
    Vector3Dd position;
    Vector3Dd up;
    Vector3Dd front;
    Vector3Dd left;
    Vector3Dd focusedPosition;
    int projectionMode;
    double fov;
    double orthogonalZoom;
    double nearPlaneDistance;
    double farPlaneDistance;

    explicit CameraState(Camera* camera);
    static bool sameVector(const Vector3Dd& a, const Vector3Dd& b);

public:
    /**
    @param camera camera whose state is captured
    @return the current state of the camera, owned by the caller
    */
    static CameraState* capture(Camera* camera);

    virtual Entity* getEntity() const override;
    virtual void restore() const override;
    virtual bool isSameState(const EntityTransformState* other) const override;
    virtual EntityTransformState* clone() const override;
};

#endif
