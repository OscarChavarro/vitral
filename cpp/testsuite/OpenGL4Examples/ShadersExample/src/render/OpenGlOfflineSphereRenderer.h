#ifndef __OPEN_GL_OFFLINE_SPHERE_RENDERER__
#define __OPEN_GL_OFFLINE_SPHERE_RENDERER__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class RGBImageUncompressed;
class ShadersModel;

class OpenGlOfflineSphereRenderer {
public:
    RGBImageUncompressed* render(
        ShadersModel* model,
        const Matrix4x4d& modelRotation,
        int width,
        int height);
};

#endif
