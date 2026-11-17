#ifndef ___OPEN_GL_4_SOLID_BOUNDARY_MESH_BUILDER__
#define ___OPEN_GL_4_SOLID_BOUNDARY_MESH_BUILDER__

#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Geometry;

/**
Builds the GL4 mesh of a solid with no parametric tessellation (quadrics,
implicit solids and height fields) from the `SolidBoundaryTessellator` mesh
of its finite bounds. The unbounded ones are clipped by the cube used for
their boundary representation (see `ImplicitSolid::DEFAULT_CLIPPING_HALF_SIZE`),
where the mesh is closed.

C++ counterpart of Java's `_Jogl4SolidBoundaryMeshBuilder`.
*/
class _OpenGL4SolidBoundaryMeshBuilder {
public:
    /// Cells along the longest side of the bounds of a tessellated solid
    static const int RESOLUTION = 64;

    /**
    @param geometry solid to tessellate
    @return a new mesh (owned by the caller) of the boundary of the solid, or
    null if its bounds can not be determined or it is empty
    */
    static OpenGL4MeshRenderer::Mesh* buildMesh(Geometry* geometry);

private:
    _OpenGL4SolidBoundaryMeshBuilder();
};

#endif
