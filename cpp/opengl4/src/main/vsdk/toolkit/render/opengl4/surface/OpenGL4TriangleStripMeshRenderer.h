#ifndef __OPEN_GL_4_TRIANGLE_STRIP_MESH_RENDERER__
#define __OPEN_GL_4_TRIANGLE_STRIP_MESH_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Camera;
class Light;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class TriangleStripMesh;

/**
Renders a `TriangleStripMesh` with the GL4 pipeline: the triangles of its
strips (see `TriangleStripMesh::exportToTriangleMesh`) are copied by
`buildMesh`, visible from both sides, with the normals of its vertexes or,
when they have none, the normal of each triangle. The mesh is cached by
`OpenGL4GeometryRenderer`, identified by the strip mesh, as they are not
edited while shown.

C++ counterpart of Java's `Jogl4TriangleStripMeshRenderer`.
*/
class OpenGL4TriangleStripMeshRenderer {
public:
    /**
    Draws the strip mesh, with the passes selected by the configuration (see
    `OpenGL4MeshRenderer`).

    @param strips strip mesh to draw
    @param camera camera that views the strip mesh
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the strip mesh
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from strip mesh space to world space
    */
    static void draw(TriangleStripMesh* strips, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param strips a strip mesh
    @return a string that identifies the mesh of the strip mesh
    */
    static java::String meshKey(TriangleStripMesh& strips);

    /**
    @param strips strip mesh to tessellate
    @return a new mesh (owned by the caller), or null if the strip mesh can not
    be tessellated
    */
    static OpenGL4MeshRenderer::Mesh* buildMesh(TriangleStripMesh& strips);

private:
    OpenGL4TriangleStripMeshRenderer();
};

#endif
