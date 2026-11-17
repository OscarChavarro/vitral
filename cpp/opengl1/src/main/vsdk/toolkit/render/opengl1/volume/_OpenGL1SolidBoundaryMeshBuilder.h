#ifndef ___OPEN_GL_1_SOLID_BOUNDARY_MESH_BUILDER__
#define ___OPEN_GL_1_SOLID_BOUNDARY_MESH_BUILDER__

#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Geometry;

/**
Builds the OpenGL 1.2 mesh of a solid with no parametric tessellation (quadrics,
implicit solids and height fields) from the `SolidBoundaryTessellator` mesh
of its finite bounds. The unbounded ones are clipped by the cube used for
their boundary representation (see `ImplicitSolid::DEFAULT_CLIPPING_HALF_SIZE`),
where the mesh is closed.

OpenGL 1.2 (fixed function) counterpart of Java's `_Jogl4SolidBoundaryMeshBuilder`, mirroring
`_OpenGL1SolidBoundaryMeshBuilder`.
*/
class _OpenGL1SolidBoundaryMeshBuilder {
public:
    /// Cells along the longest side of the bounds of a tessellated solid
    static const int RESOLUTION = 64;

    /**
    @param geometry solid to tessellate
    @return a new mesh (owned by the caller) of the boundary of the solid, or
    null if its bounds can not be determined or it is empty
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(Geometry* geometry);

private:
    _OpenGL1SolidBoundaryMeshBuilder();
};

#endif
