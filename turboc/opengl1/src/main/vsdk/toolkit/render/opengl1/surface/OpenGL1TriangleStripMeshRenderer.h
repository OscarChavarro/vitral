#ifndef __OGL1_TRI_STRIP_MESH_RNDR__
#define __OGL1_TRI_STRIP_MESH_RNDR__

#include "java/lang/String.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
class TriangleStripMesh;

/**
Builds the OpenGL 1.2 meshes of triangle strip meshes, drawn by
`OpenGL1GeometryRenderer` (as `Jogl4TriangleStripMeshRenderer` of the Java
OpenGL 4 port): the triangles of `TriangleStripMesh::exportToTriangleMesh`,
visible from both sides.
*/
class OpenGL1TriangleStripMeshRenderer {
private:
    OpenGL1TriangleStripMeshRenderer();

public:
    /**
    @return a string that identifies the mesh of the strips, by identity
    */
    static String meshKey(TriangleStripMesh& strips);

    /**
    @return a new mesh, owned by the caller, or null if there are no
    triangles. Vertices without a normal get the normal of their triangle.
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(TriangleStripMesh& strips);
};

#endif
