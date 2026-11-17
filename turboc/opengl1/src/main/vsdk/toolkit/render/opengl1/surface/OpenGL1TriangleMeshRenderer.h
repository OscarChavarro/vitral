#ifndef __OGL1_TRIANGLE_MESH_RENDERER__
#define __OGL1_TRIANGLE_MESH_RENDERER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
class OpenGL1MeshBuilder;
class TriangleMesh;

/**
Builds the OpenGL 1.2 meshes of triangle meshes, drawn by
`OpenGL1GeometryRenderer` (as `Jogl4TriangleMeshRenderer` of the Java
OpenGL 4 port).
*/
class OpenGL1TriangleMeshRenderer {
private:
    OpenGL1TriangleMeshRenderer();

public:
    /**
    @return a string that identifies the mesh of the triangle mesh: by
    identity, as triangle meshes are not edited while shown
    */
    static String meshKey(TriangleMesh& mesh);

    /**
    @return a new mesh with the triangles of the mesh, owned by the caller,
    or null if it has no triangles
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(TriangleMesh& mesh);

    /**
    @param doubleSided true to add a back side to each triangle
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(TriangleMesh& mesh,
        BOOLEAN doubleSided);

    /**
    Adds the triangles of a mesh to a builder: normals the mesh does not
    have are (0, 0, 1), and texture coordinates (0, 0).
    */
    static void addTriangles(OpenGL1MeshBuilder& builder, TriangleMesh& mesh);
};

#endif
