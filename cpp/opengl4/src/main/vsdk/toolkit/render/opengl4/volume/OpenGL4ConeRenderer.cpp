#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <algorithm>
#include <cstdarg>
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4ConeRenderer.h"

void OpenGL4ConeRenderer::draw(Cone* cone, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL4GeometryRenderer::draw(cone, camera, lights, material, quality,
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

java::String OpenGL4ConeRenderer::meshKey(const Cone& cone)
{
    return format("cone/%.17g/%.17g/%.17g", cone.getBottomRadius(),
        cone.getTopRadius(), cone.getHeight());
}

OpenGL4MeshRenderer::Mesh* OpenGL4ConeRenderer::buildMesh(const Cone& cone)
{
    double r1 = cone.getBottomRadius();
    double r2 = cone.getTopRadius();
    double h = cone.getHeight();
    OpenGL4MeshBuilder builder(std::max(std::max(r1, r2), h));

    builder.addFrustum(0, r1, h, r2, SLICES);
    builder.addDisk(0, 0, r1, false, SLICES);
    if ( r2 > 0.0 ) {
        builder.addDisk(h, 0, r2, true, SLICES);
    }
    return builder.build();
}
