#include "java/lang/Math.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <cstdarg>
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1TubeRenderer.h"

const int OpenGL1TubeRenderer::SLICES;

void OpenGL1TubeRenderer::draw(Tube* tube, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(tube, camera, lights, material, quality,
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

java::String OpenGL1TubeRenderer::meshKey(const Tube& tube)
{
    return format("tube/%.17g/%.17g/%.17g", tube.getInternalRadius(),
        tube.getExternalRadius(), tube.getHeight());
}

OpenGL1MeshRenderer::Mesh* OpenGL1TubeRenderer::buildMesh(const Tube& tube)
{
    double ri = tube.getInternalRadius();
    double re = tube.getExternalRadius();
    double h = tube.getHeight();
    OpenGL1MeshBuilder builder(java::Math::max(re, h));

    if ( tube.isWasher() ) {
        builder.addDisk(0, ri, re, true, SLICES);
        builder.setDoubleSided(true);
        return builder.build();
    }
    builder.addFrustum(0, re, h, re, SLICES);
    if ( ri > 0.0 ) {
        addInternalWall(builder, ri, h);
    }
    builder.addDisk(0, ri, re, false, SLICES);
    builder.addDisk(h, ri, re, true, SLICES);
    return builder.build();
}

void OpenGL1TubeRenderer::addInternalWall(OpenGL1MeshBuilder& builder,
    double r, double h)
{
    for ( int i = 0; i < SLICES; i++ ) {
        double a0 = 2 * M_PI * i / SLICES;
        double a1 = 2 * M_PI * (i + 1) / SLICES;
        double c0 = std::cos(a0);
        double s0 = std::sin(a0);
        double c1 = std::cos(a1);
        double s1 = std::sin(a1);
        Vector3Dd n0(-c0, -s0, 0);
        Vector3Dd n1(-c1, -s1, 0);
        double u0 = (double)i / SLICES;
        double u1 = (double)(i + 1) / SLICES;

        builder.addQuad(
            Vector3Dd(r * c1, r * s1, 0), n1, u1, 0,
            Vector3Dd(r * c0, r * s0, 0), n0, u0, 0,
            Vector3Dd(r * c0, r * s0, h), n0, u0, 1,
            Vector3Dd(r * c1, r * s1, h), n1, u1, 1);
    }
}
