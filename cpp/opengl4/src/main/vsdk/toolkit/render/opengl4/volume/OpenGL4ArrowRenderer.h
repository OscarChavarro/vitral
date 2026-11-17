#ifndef __VITRAL_OPEN_GL_4_ARROW_RENDERER__
#define __VITRAL_OPEN_GL_4_ARROW_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Arrow;
class Camera;
class Light;
class RendererConfiguration;
class SimpleMaterial;

/**
Renders an `Arrow` with the GL4 pipeline, in two ways: `draw` with its own
mesh of `SLICES` divisions (as needed by the gizmos), and, through
`OpenGL4GeometryRenderer`, from the mesh built by `buildMesh` (cached by its
parameters), that honors every bit of the `RendererConfiguration` as the
other geometries.
*/
class OpenGL4ArrowRenderer {
public:
    /// Divisions around the axis of the mesh built by `buildMesh`
    static const int MESH_SLICES = 32;

    static void draw(
        const Arrow* arrow,
        const Matrix4x4d& modelMatrix,
        const Matrix4x4d& projection,
        const Camera* camera,
        const java::ArrayList<Light*>& lights,
        const SimpleMaterial* material,
        const RendererConfiguration* quality);

    static void dispose();

    /**
    @param arrow an arrow
    @return a string that identifies the mesh of the arrow
    */
    static java::String meshKey(const Arrow& arrow);

    /**
    @param arrow arrow to tessellate
    @return a new mesh (owned by the caller) of the base cylinder and the
    head cone of the arrow
    */
    static OpenGL4MeshRenderer::Mesh* buildMesh(const Arrow& arrow);

private:
    struct ArrowMesh {
        java::ArrayList<float> positions;
        java::ArrayList<float> normals;
        java::ArrayList<float> uvs;
        int vertexCount;
    };

    static const int SLICES;
    static unsigned int vao;
    static unsigned int vboPositions;
    static unsigned int vboNormals;
    static unsigned int vboUvs;
    static unsigned int program;
    static int vertexCount;
    static bool initialized;

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
