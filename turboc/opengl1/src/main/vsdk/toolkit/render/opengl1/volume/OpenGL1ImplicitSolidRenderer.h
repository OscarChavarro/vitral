#ifndef __OGL1_IMPLICIT_SOLID_RNDR__
#define __OGL1_IMPLICIT_SOLID_RNDR__

#include "java/lang/String.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
class ImplicitSolid;

/**
Builds the OpenGL 1.2 meshes of implicit solids (blobs and polynomial
surfaces), drawn by `OpenGL1GeometryRenderer` (as
`Jogl4ImplicitSolidRenderer` of the Java OpenGL 4 port), with
`_OpenGL1SolidBoundaryMeshBuilder`.
*/
class OpenGL1ImplicitSolidRenderer {
private:
    OpenGL1ImplicitSolidRenderer();

public:
    /**
    @return a string that identifies the mesh: by the parameters of blobs
    and polynomial surfaces, by identity for other implicit solids
    */
    static String meshKey(ImplicitSolid& solid);

    static OpenGL1MeshRenderer::Mesh* buildMesh(ImplicitSolid& solid);
};

#endif
