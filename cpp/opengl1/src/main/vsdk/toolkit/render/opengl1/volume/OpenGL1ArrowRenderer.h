#ifndef __VITRAL_OPEN_GL_1_ARROW_RENDERER__
#define __VITRAL_OPEN_GL_1_ARROW_RENDERER__

#include "java/util/ArrayList.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Arrow;
class Camera;
class Light;
class RendererConfiguration;
class SimpleMaterial;

/**
Renders an `Arrow` with the OpenGL 1.2 pipeline, in two ways: `draw` with its
own mesh (as needed by the gizmos), and, through `OpenGL1GeometryRenderer`,
from the mesh built by `buildMesh` (cached by its parameters), that honors
every bit of the `RendererConfiguration` as the other geometries.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4ArrowRenderer`,
mirroring `OpenGL4ArrowRenderer`.
*/
class OpenGL1ArrowRenderer {
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
    static OpenGL1MeshRenderer::Mesh* buildMesh(const Arrow& arrow);

private:
    struct ArrowMesh {
        java::ArrayList<float> positions;
        java::ArrayList<float> normals;
        java::ArrayList<float> uvs;
        int vertexCount;
    };

    static const int SLICES;
    static unsigned int displayListId;
    static int vertexCount;
    static bool initialized;

    static bool ensureMesh(const Arrow* arrow);
    static ArrowMesh buildArrowMesh(
        double baseRadius,
        double headRadius,
        double baseLength,
        double headLength,
        int slices);
    static void uploadMesh(const ArrowMesh& mesh);
    static void addPos(java::ArrayList<float>& buf, float x, float y, float z);
    static void addNorm(java::ArrayList<float>& buf, float x, float y, float z);
    static void addUv(java::ArrayList<float>& buf, float u, float v);
};

#endif
