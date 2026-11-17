#ifndef __OPEN_GL_1_TRIANGLE_MESH_RENDERER__
#define __OPEN_GL_1_TRIANGLE_MESH_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class Light;
class OpenGL1MeshBuilder;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class TriangleMesh;

/**
Renders a `TriangleMesh` with the OpenGL 1.2 pipeline: its triangles are copied
by `buildMesh` with their normals and texture coordinates, and the mesh is
cached by `OpenGL1GeometryRenderer`, identified by the triangle mesh, as they
are not edited while shown. It is also the way to draw the triangle meshes
that other geometries are tessellated to.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4TriangleMeshRenderer`, mirroring
`OpenGL1TriangleMeshRenderer`.
*/
class OpenGL1TriangleMeshRenderer {
public:
    /**
    Draws the mesh, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param mesh mesh to draw
    @param camera camera that views the mesh
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the mesh
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from mesh space to world space
    */
    static void draw(TriangleMesh* mesh, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param mesh a mesh
    @return a string that identifies the mesh of the mesh
    */
    static java::String meshKey(TriangleMesh& mesh);

    /**
    @param mesh mesh to tessellate
    @return a new mesh (owned by the caller), or null if the mesh can not
    be tessellated
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(TriangleMesh& mesh);

    /**
    @param mesh mesh to copy
    @param doubleSided true to make the mesh visible from both sides
    @return a new mesh (owned by the caller), or null if it has no vertices
    or triangles
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(TriangleMesh& mesh,
                                                bool doubleSided);

private:
    OpenGL1TriangleMeshRenderer();
    static void addTriangles(OpenGL1MeshBuilder& builder, TriangleMesh& mesh);
};

#endif
