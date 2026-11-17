#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1HeightFieldSolidRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/_OpenGL1SolidBoundaryMeshBuilder.h"

String OpenGL1HeightFieldSolidRenderer::meshKey(HeightFieldSolid& solid)
{
    char buffer[80];
    sprintf(buffer, "tessellatedsolid/%p/%d", (void*)&solid,
        (int)_OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
    return String(buffer);
}

OpenGL1MeshRenderer::Mesh* OpenGL1HeightFieldSolidRenderer::buildMesh(
    HeightFieldSolid& solid)
{
    return _OpenGL1SolidBoundaryMeshBuilder::buildMesh(&solid);
}
