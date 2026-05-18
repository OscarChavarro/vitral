#ifndef SHADERSEXAMPLE_RENDER_SOFTWARERAYCASTER_H
#define SHADERSEXAMPLE_RENDER_SOFTWARERAYCASTER_H

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/camera/Camera.h"

class ShadersModel;

class SoftwareRaycaster {
public:
    void invalidateSnapshot();
    void render(
        ShadersModel* model,
        Camera* activeCamera,
        const Matrix4x4d& modelRotation);
};

#endif
