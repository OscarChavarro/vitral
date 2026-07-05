#ifndef __OPENGL4SPHERERENDERER__
#define __OPENGL4SPHERERENDERER__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
class Sphere;
class Light;
class SimpleMaterial;
class RendererConfiguration;
class RGBImageUncompressed;
class Camera;

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
    static unsigned int vboTangents_;
    static unsigned int vboBinormals_;
    static unsigned int ebo_;
    static unsigned int constantProgram_;
    static unsigned int texturedProgram_;
    static unsigned int flatProgram_;
    static unsigned int flatTexturedProgram_;
    static unsigned int gouraudProgram_;
    static unsigned int phongProgram_;
    static unsigned int phongBumpProgram_;
    static unsigned int cookProgram_;
    static unsigned int cookBumpProgram_;

    static int cachedMeridians_;
    static int cachedParallels_;
    static unsigned int indexCount_;

    static bool initProgramIfNeeded();
    static bool buildSphereMeshIfNeeded(int meridians, int parallels);
    static unsigned int selectProgram(const RendererConfiguration* quality, bool hasTexture, bool hasNormalMap);
    static java::String readTextFile(const java::String& path);
    static java::String findShaderSource(const java::String& shaderFileName);
    static unsigned int buildProgram(const char* vsFile, const char* fsFile);
    static unsigned int compileShader(unsigned int type, const char* source);
    static void setUniform3f(unsigned int program, const char* name, const Vector3Dd& v);
    static void setUniform3f(unsigned int program, const char* name, const ColorRgb& c);
    static void setUniform1i(unsigned int program, const char* name, int v);
    static void setUniform1f(unsigned int program, const char* name, float v);
    static void configureMicrofacetUniforms(unsigned int programId, const SimpleMaterial* material);
};

#endif
