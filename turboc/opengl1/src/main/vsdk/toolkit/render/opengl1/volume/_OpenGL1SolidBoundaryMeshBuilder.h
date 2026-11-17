#ifndef __OGL1_SOLID_BNDRY_MESH_BLDR__
#define __OGL1_SOLID_BNDRY_MESH_BLDR__

#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
class Geometry;

/**
Builds the OpenGL 1.2 mesh of a solid without a parametric tessellation
(i.e. an implicit or quadric surface, or a height field) from the boundary
found by `SolidBoundaryTessellator` (as `_Jogl4SolidBoundaryMeshBuilder` of
the Java OpenGL 4 port). Unbounded solids are clipped by a cube of half side
`ImplicitSolid::DEFAULT_CLIPPING_HALF_SIZE`.
*/
class _OpenGL1SolidBoundaryMeshBuilder {
private:
    _OpenGL1SolidBoundaryMeshBuilder();

public:
    /// Cells along the longest side of the bounds of a tessellated solid
    enum { RESOLUTION = 64 };

    /**
    @return a new mesh, owned by the caller, or null if the solid has no
    finite bounds or its boundary can not be tessellated
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(Geometry* geometry);
};

#endif
