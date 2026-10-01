#ifndef __OPEN_GL_1_SPHERE_RENDERER__
#define __OPEN_GL_1_SPHERE_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/SpherePolyhedralCache.h"
class Sphere;
class Light;
class SimpleMaterial;
class RendererConfiguration;
class RGBImageUncompressed;
class Camera;

class OpenGL1SphereRenderer {
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
    static const SpherePolyhedralCache::Entry* obtainTessellation(int meridians, int parallels);
    static void drawElements(const SpherePolyhedralCache::Entry* entry);
};

#endif
