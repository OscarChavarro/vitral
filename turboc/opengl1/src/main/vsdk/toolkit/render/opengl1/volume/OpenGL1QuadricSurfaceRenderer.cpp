#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1QuadricSurfaceRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/_OpenGL1SolidBoundaryMeshBuilder.h"

String OpenGL1QuadricSurfaceRenderer::meshKey(QuadricSurface& quadric)
{
    char buffer[512];
    const Vector3Dd& s = quadric.squareCoefficients();
    const Vector3Dd& m = quadric.mixedCoefficients();
    const Vector3Dd& l = quadric.linearCoefficients();
    sprintf(buffer, "quadric/%.17g,%.17g,%.17g/%.17g,%.17g,%.17g/"
        "%.17g,%.17g,%.17g/%.17g/%d", s.x(), s.y(), s.z(), m.x(), m.y(),
        m.z(), l.x(), l.y(), l.z(), quadric.constantCoefficient(),
        (int)_OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
    return String(buffer);
}

OpenGL1MeshRenderer::Mesh* OpenGL1QuadricSurfaceRenderer::buildMesh(
    QuadricSurface& quadric)
{
    return _OpenGL1SolidBoundaryMeshBuilder::buildMesh(&quadric);
}
