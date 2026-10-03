#ifndef __OPEN_GL_4_POLYHEDRAL_BOUNDED_SOLID_RENDERER__
#define __OPEN_GL_4_POLYHEDRAL_BOUNDED_SOLID_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Camera;
class Light;
class PolyhedralBoundedSolid;
class RendererConfiguration;
class SimpleMaterial;

/**
Draws a `PolyhedralBoundedSolid` (boundary representation of [MANT1988])
with the GL4 core pipeline: its faces are tessellated with the GLU
tessellator (they are planar polygons, maybe with holes) and drawn with the
GLSL programs of the shading type of the configuration (flat shading is
evaluated per triangle on the CPU), followed by the debugging overlays of
`OpenGL4PolyhedralBoundedSolidDebugRenderer` (wires, points, normals,
bounding volume, selection corners and non planar faces).

The material and the lights are not parameters: they are the ones activated
with `OpenGL4SimpleMaterialRenderer` and `OpenGL4LightRenderer`. Textures
and normal maps are not supported.

C++ counterpart of Java's `Jogl4PolyhedralBoundedSolidRenderer`. C++ port
notes: the OpenGL context is the current one (there is no `gl` parameter),
and the arrays of floats are `java::ArrayList<float>`.
*/
class OpenGL4PolyhedralBoundedSolidRenderer {
public:
    /**
    Triangles of a tessellated solid: positions (x, y, z, 1), normals and
    texture coordinates of each vertex.
    */
    struct MeshData {
        java::ArrayList<float> positions;
        java::ArrayList<float> normals;
        java::ArrayList<float> uvs;
        int vertexCount;

        MeshData();
    };

    /**
    Draws the solid with the identity as model matrix.
    */
    static void draw(PolyhedralBoundedSolid* solid, Camera* camera,
                     const RendererConfiguration* quality);

    /**
    @param solid solid to draw
    @param camera camera that views the solid
    @param quality bits of rendering configuration
    @param modelMatrix transformation from the space of the solid to world
    space
    */
    static void draw(PolyhedralBoundedSolid* solid, Camera* camera,
                     const RendererConfiguration* quality,
                     const Matrix4x4d& modelMatrix);

    static void drawDebugFaceBoundary(PolyhedralBoundedSolid* solid,
                                      int faceIndex,
                                      const Matrix4x4d& modelViewProjection);

    static void drawDebugFace(PolyhedralBoundedSolid* solid, int faceIndex,
                              const Matrix4x4d& modelMatrix,
                              const Matrix4x4d& modelViewProjection,
                              Camera* camera);

    static void drawDebugEdges(PolyhedralBoundedSolid* solid, Camera* camera,
                               int edgeIndex,
                               const Matrix4x4d& modelViewProjection);

    /**
    Releases the OpenGL objects. PRE: the context that built them is current.
    */
    static void release();

    //= Used by OpenGL4PolyhedralBoundedSolidDebugRenderer ===============
    // (package private in Java)

    static void ensureInitialized();

    static void configureSurfaceProgram(
        unsigned int programId,
        const Matrix4x4d& modelViewProjection,
        const Matrix4x4d& modelViewLocal,
        const Matrix4x4d& modelViewITLocal,
        const SimpleMaterial& material,
        const java::ArrayList<Light*>* lights,
        const RendererConfiguration* quality,
        const Vector3Dd& cameraPosition);

    static void renderMesh(const MeshData& mesh, unsigned int mode);

    static void drawColoredPrimitives(
        const Matrix4x4d& mvp,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        unsigned int mode,
        float size,
        float depthBiasNdc);

    static void buildFaceMesh(PolyhedralBoundedSolid* solid, int faceIndex,
                              MeshData& outMesh);

private:
    static bool initialized;
    static unsigned int meshVaoId;
    static unsigned int meshPositionVboId;
    static unsigned int meshNormalVboId;
    static unsigned int meshUvVboId;
    static unsigned int colorVaoId;
    static unsigned int colorPositionVboId;
    static unsigned int colorDataVboId;
    static unsigned int colorProgramId;

    static void draw(PolyhedralBoundedSolid* solid, Camera* camera,
                     const RendererConfiguration* quality,
                     const Matrix4x4d& modelMatrix,
                     const Matrix4x4d& modelViewProjection);

    OpenGL4PolyhedralBoundedSolidRenderer() {}
};

#endif
