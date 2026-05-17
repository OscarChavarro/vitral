#ifndef SHADERSEXAMPLE_RENDER_OPENGLOFFLINESPHERERENDERER_H
#define SHADERSEXAMPLE_RENDER_OPENGLOFFLINESPHERERENDERER_H

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
