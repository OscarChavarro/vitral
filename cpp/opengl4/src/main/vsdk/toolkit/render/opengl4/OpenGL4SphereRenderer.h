#ifndef __OPEN_GL_4_SPHERE_RENDERER__
#define __OPEN_GL_4_SPHERE_RENDERER__

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
    static unsigned int vao;
    static unsigned int vboPositions;
    static unsigned int vboNormals;
    static unsigned int vboUvs;
    static unsigned int vboTangents;
    static unsigned int vboBinormals;
    static unsigned int ebo;
    static unsigned int constantProgram;
    static unsigned int texturedProgram;
    static unsigned int flatProgram;
    static unsigned int flatTexturedProgram;
    static unsigned int gouraudProgram;
    static unsigned int phongProgram;
    static unsigned int phongBumpProgram;
    static unsigned int cookProgram;
    static unsigned int cookBumpProgram;

    static int cachedMeridians;
    static int cachedParallels;
    static unsigned int indexCount;

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
