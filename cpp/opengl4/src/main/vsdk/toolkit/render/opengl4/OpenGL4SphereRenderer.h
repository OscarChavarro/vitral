#ifndef __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4SPHERERENDERER_H__
#define __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4SPHERERENDERER_H__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Sphere;
class Light;
class SimpleMaterial;
class RendererConfiguration;
class RGBImageUncompressed;
class Camera;

namespace vsdk { namespace toolkit { namespace render { namespace opengl4 {

class OpenGL4SphereRenderer {
public:
    static void draw(
        const Sphere* sphere,
        const Camera* camera,
        const Light* light,
        const SimpleMaterial* material,
        const RendererConfiguration* quality,
        RGBImageUncompressed* textureMap,
        RGBImageUncompressed* bumpMapHeightRgb,
        const Matrix4x4d& modelRotation,
        int meridians,
        int parallels);

    static void dispose();

private:
    static unsigned int vao_;
    static unsigned int vboPositions_;
    static unsigned int vboNormals_;
    static unsigned int vboUvs_;
    static unsigned int ebo_;
    static unsigned int program_;

    static int cachedMeridians_;
    static int cachedParallels_;
    static unsigned int indexCount_;

    static bool initProgramIfNeeded();
    static bool buildSphereMeshIfNeeded(int meridians, int parallels);
    static unsigned int compileShader(unsigned int type, const char* source);
};

}}}}

#endif
