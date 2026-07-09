#ifndef VITRAL_OPEN_GL_4_ARROW_RENDERER_H
#define VITRAL_OPEN_GL_4_ARROW_RENDERER_H

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Arrow;
class Camera;
class Light;
class RendererConfiguration;
class SimpleMaterial;

class OpenGL4ArrowRenderer {
public:
    static void draw(
        const Arrow* arrow,
        const Matrix4x4d& modelMatrix,
        const Matrix4x4d& projection,
        const Camera* camera,
        const java::ArrayList<Light*>& lights,
        const SimpleMaterial* material,
        const RendererConfiguration* quality);

    static void dispose();

private:
    struct ArrowMesh {
        java::ArrayList<float> positions;
        java::ArrayList<float> normals;
        java::ArrayList<float> uvs;
        int vertexCount;
    };

    static const int SLICES;
    static unsigned int vao_;
    static unsigned int vboPositions_;
    static unsigned int vboNormals_;
    static unsigned int vboUvs_;
    static unsigned int program_;
    static int vertexCount_;
    static bool initialized_;

    static bool ensureProgram();
    static bool ensureMesh(const Arrow* arrow);
    static ArrowMesh buildArrowMesh(
        double baseRadius,
        double headRadius,
        double baseLength,
        double headLength,
        int slices);
    static void uploadMesh(const ArrowMesh& mesh);
    static unsigned int buildProgram(const char* vsFile, const char* fsFile);
    static unsigned int compileShader(unsigned int type, const char* source);
    static java::String readTextFile(const java::String& path);
    static java::String findShaderSource(const java::String& shaderFileName);
    static void addPos(java::ArrayList<float>& buf, float x, float y, float z);
    static void addNorm(java::ArrayList<float>& buf, float x, float y, float z);
    static void addUv(java::ArrayList<float>& buf, float u, float v);
    static void uploadBuffer(unsigned int bufferId, int attrib, int size, const java::ArrayList<float>& data);
    static void setMatrix(unsigned int programId, const char* name, const Matrix4x4d& matrix);
    static void setVector3(unsigned int programId, const char* name, const Vector3Dd& value);
    static void setVector3(unsigned int programId, const char* name, const ColorRgb& value);
    static void setInt(unsigned int programId, const char* name, int value);
    static void setFloat(unsigned int programId, const char* name, float value);
};

#endif
