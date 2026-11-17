#ifndef __OGL1_HEIGHTFIELD_SOLID_RNDR__
#define __OGL1_HEIGHTFIELD_SOLID_RNDR__

#include "java/lang/String.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
class HeightFieldSolid;

/**
Builds the OpenGL 1.2 meshes of height field solids, drawn by
`OpenGL1GeometryRenderer` (as `Jogl4HeightFieldSolidRenderer` of the Java
OpenGL 4 port), with `_OpenGL1SolidBoundaryMeshBuilder`.
*/
class OpenGL1HeightFieldSolidRenderer {
private:
    OpenGL1HeightFieldSolidRenderer();

public:
    /**
    @return a string that identifies the mesh of the solid, by identity
    */
    static String meshKey(HeightFieldSolid& solid);

    static OpenGL1MeshRenderer::Mesh* buildMesh(HeightFieldSolid& solid);
};

#endif
