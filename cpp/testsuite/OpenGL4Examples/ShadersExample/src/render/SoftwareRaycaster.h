#ifndef __SOFTWARE_RAYCASTER__
#define __SOFTWARE_RAYCASTER__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
class ShadersModel;
class NormalMap;
class SimpleSceneSnapshot;
class RGBImageUncompressed;
class ParallelRaytracer;

class SoftwareRaycaster {
public:
    SoftwareRaycaster();
    ~SoftwareRaycaster();

    void invalidateSnapshot();
    void render(
        ShadersModel* model,
        Camera* activeCamera,
        const Matrix4x4d& modelRotation);

private:
    /** One thread per available processor, reused by every frame */
    ParallelRaytracer* parallelRaytracer;
    NormalMap* bumpNormalMap;

    SimpleSceneSnapshot* buildSceneSnapshot(
        ShadersModel* model,
        Camera* activeCamera,
        const Matrix4x4d& modelRotation,
        RGBImageUncompressed* outputImage);
};

#endif
