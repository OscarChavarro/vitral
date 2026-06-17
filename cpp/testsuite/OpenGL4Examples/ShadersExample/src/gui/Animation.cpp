#include <cmath>

#include "../model/ShadersModel.h"
#include "Animation.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/light/Light.h"
Animation::Animation()
    : lastSphereTickSeconds(-1.0),
      lastLightTickSeconds(-1.0),
      lightAnimationAngleRadians(0.0) {}

void Animation::reset()
{
    lastSphereTickSeconds = -1.0;
    lastLightTickSeconds = -1.0;
}

void Animation::tick(ShadersModel* model, double nowSeconds)
{
    if (model == 0 || !model->animationEnabled) {
        reset();
        return;
    }
    if (lastSphereTickSeconds < 0.0) {
        lastSphereTickSeconds = nowSeconds;
        return;
    }
    double elapsed = nowSeconds - lastSphereTickSeconds;
    lastSphereTickSeconds = nowSeconds;
    if (elapsed < 0.0) return;
    if (elapsed > 0.25) elapsed = 0.25;
    const double angularSpeed = (2.0 * M_PI) / 8.0;
    model->advanceSphereRotationRadians(angularSpeed * elapsed);
}

void Animation::tickForApp(
    double* sphereAngleRadians,
    bool animationEnabled,
    Light* light,
    bool lightAnimationEnabled,
    double nowSeconds)
{
    if (!animationEnabled) {
        lastSphereTickSeconds = -1.0;
    }
    else if (sphereAngleRadians) {
        if (lastSphereTickSeconds < 0.0) {
            lastSphereTickSeconds = nowSeconds;
        }
        else {
            double elapsed = nowSeconds - lastSphereTickSeconds;
            lastSphereTickSeconds = nowSeconds;
            if (elapsed >= 0.0) {
                if (elapsed > 0.25) elapsed = 0.25;
                *sphereAngleRadians += ((2.0 * M_PI) / 8.0) * elapsed;
            }
        }
    }

    if (!lightAnimationEnabled || light == 0) {
        lastLightTickSeconds = -1.0;
        return;
    }
    if (lastLightTickSeconds < 0.0) {
        lastLightTickSeconds = nowSeconds;
        return;
    }
    double elapsed = nowSeconds - lastLightTickSeconds;
    lastLightTickSeconds = nowSeconds;
    if (elapsed < 0.0) return;
    if (elapsed > 0.25) elapsed = 0.25;

    lightAnimationAngleRadians += ((2.0 * M_PI) / 8.0) * elapsed;
    Matrix4x4d rotation = Matrix4x4d().axisRotation(lightAnimationAngleRadians, 0.0, -1.0, 0.0);
    Vector3Dd baseLightPosition(1.0, -3.0, 1.0);
    light->setPosition(rotation.multiply(baseLightPosition));
}
