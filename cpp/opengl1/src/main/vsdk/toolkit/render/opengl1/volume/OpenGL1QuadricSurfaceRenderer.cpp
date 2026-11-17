#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <cstdarg>
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/render/opengl1/volume/_OpenGL1SolidBoundaryMeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1QuadricSurfaceRenderer.h"

void OpenGL1QuadricSurfaceRenderer::draw(QuadricSurface* quadric, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(quadric, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

namespace {

java::String format(const char* pattern, ...)
    __attribute__((format(printf, 1, 2)));

java::String format(const char* pattern, ...)
{
    char buffer[512];
    va_list args;
    va_start(args, pattern);
    vsnprintf(buffer, sizeof(buffer), pattern, args);
    va_end(args);
    return buffer;
}

}

java::String OpenGL1QuadricSurfaceRenderer::meshKey(QuadricSurface& quadric)
{
    return java::String("quadric/") + keyOf(quadric.squareCoefficients()) + "/" +
        keyOf(quadric.mixedCoefficients()) + "/" +
        keyOf(quadric.linearCoefficients()) + "/" +
        format("%.17g/%d", quadric.constantCoefficient(),
            _OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
}

OpenGL1MeshRenderer::Mesh* OpenGL1QuadricSurfaceRenderer::buildMesh(
    QuadricSurface& quadric)
{
    return _OpenGL1SolidBoundaryMeshBuilder::buildMesh(&quadric);
}

java::String OpenGL1QuadricSurfaceRenderer::keyOf(const Vector3Dd& v)
{
    return format("%.17g,%.17g,%.17g", v.x(), v.y(), v.z());
}
