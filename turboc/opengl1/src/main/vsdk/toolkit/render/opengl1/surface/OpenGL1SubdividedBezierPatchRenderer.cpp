#include <stddef.h>
#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/processing/BezierPatchTessellator.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1SubdividedBezierPatchRenderer.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshRenderer.h"

String OpenGL1SubdividedBezierPatchRenderer::meshKey(SubdividedBezierPatch& patch)
{
    char buffer[80];
    sprintf(buffer, "subdividedbezierpatch/%p/%d", (void*)&patch,
        (int)TESSELLATION_STEPS);
    return String(buffer);
}

/**
C++ port note: the tessellated mesh is deleted here.
*/
OpenGL1MeshRenderer::Mesh* OpenGL1SubdividedBezierPatchRenderer::buildMesh(
    SubdividedBezierPatch& patch)
{
    TriangleMesh* mesh = BezierPatchTessellator::tessellate(patch,
        TESSELLATION_STEPS, TESSELLATION_STEPS);
    if ( mesh == NULL ) {
        return NULL;
    }
    OpenGL1MeshRenderer::Mesh* result =
        OpenGL1TriangleMeshRenderer::buildMesh(*mesh, TRUE);
    delete mesh;
    return result;
}
