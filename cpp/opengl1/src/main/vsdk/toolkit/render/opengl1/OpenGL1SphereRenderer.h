#ifndef __OPEN_GL_1_SPHERE_RENDERER__
#define __OPEN_GL_1_SPHERE_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
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
    // Unit sphere, drawn from client side vertex arrays
    static java::ArrayList<float> positions;
    static java::ArrayList<float> normals;
    static java::ArrayList<float> uvs;
    static java::ArrayList<unsigned int> indices;

    static int cachedMeridians;
    static int cachedParallels;
    static unsigned int indexCount;

    static bool buildSphereMeshIfNeeded(int meridians, int parallels);
    static void drawElements();
};

#endif
