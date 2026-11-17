#ifndef __OGL1_QUADRIC_SURFACE_RNDR__
#define __OGL1_QUADRIC_SURFACE_RNDR__

#include "java/lang/String.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
class QuadricSurface;

/**
Builds the OpenGL 1.2 meshes of quadric surfaces, drawn by
`OpenGL1GeometryRenderer` (as `Jogl4QuadricSurfaceRenderer` of the Java
OpenGL 4 port), with `_OpenGL1SolidBoundaryMeshBuilder`.
*/
class OpenGL1QuadricSurfaceRenderer {
private:
    OpenGL1QuadricSurfaceRenderer();

public:
    /**
    @return a string that identifies the mesh, by the coefficients of the
    quadric
    */
    static String meshKey(QuadricSurface& quadric);

    static OpenGL1MeshRenderer::Mesh* buildMesh(QuadricSurface& quadric);
};

#endif
