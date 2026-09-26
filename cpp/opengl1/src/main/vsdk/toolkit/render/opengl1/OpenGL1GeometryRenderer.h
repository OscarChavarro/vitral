#ifndef __OPEN_GL_1_GEOMETRY_RENDERER__
#define __OPEN_GL_1_GEOMETRY_RENDERER__
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class Camera; class Geometry; class Light; class RendererConfiguration; class SimpleMaterial; class RGBImageUncompressed;
class OpenGL1GeometryRenderer {
public:
    static void draw(Geometry* geometry, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);
    static void dispose();
};
#endif
