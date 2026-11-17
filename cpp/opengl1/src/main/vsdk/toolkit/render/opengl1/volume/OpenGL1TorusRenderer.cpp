#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <cstdarg>
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/Torus.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1TorusRenderer.h"

void OpenGL1TorusRenderer::draw(Torus* torus, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(torus, camera, lights, material, quality,
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

java::String OpenGL1TorusRenderer::meshKey(const Torus& torus)
{
    return format("torus/%.17g/%.17g", torus.getMajorRadius(),
        torus.getMinorRadius());
}

OpenGL1MeshRenderer::Mesh* OpenGL1TorusRenderer::buildMesh(const Torus& torus)
{
    double bigR = torus.getMajorRadius();
    double smallR = torus.getMinorRadius();
    OpenGL1MeshBuilder builder(bigR + smallR);
    int nw = MAJOR_SEGMENTS;
    int nv = MINOR_SEGMENTS;

    for ( int i = 0; i < nw; i++ ) {
        double w0 = 2 * M_PI * i / nw;
        double w1 = 2 * M_PI * (i + 1) / nw;
        for ( int j = 0; j < nv; j++ ) {
            double v0 = 2 * M_PI * j / nv;
            double v1 = 2 * M_PI * (j + 1) / nv;
            Vector3Dd p00 = point(bigR, smallR, w0, v0);
            Vector3Dd p01 = point(bigR, smallR, w0, v1);
            Vector3Dd p11 = point(bigR, smallR, w1, v1);
            Vector3Dd p10 = point(bigR, smallR, w1, v0);
            Vector3Dd n00 = normal(w0, v0);
            Vector3Dd n01 = normal(w0, v1);
            Vector3Dd n11 = normal(w1, v1);
            Vector3Dd n10 = normal(w1, v0);
            builder.addQuad(
                p00, n00, (double)i / nw, (double)j / nv,
                p10, n10, (double)(i + 1) / nw, (double)j / nv,
                p11, n11, (double)(i + 1) / nw, (double)(j + 1) / nv,
                p01, n01, (double)i / nw, (double)(j + 1) / nv);
        }
    }
    return builder.build();
}

Vector3Dd OpenGL1TorusRenderer::point(double bigR, double smallR, double w,
                                      double v)
{
    return Vector3Dd(
        (bigR + smallR * std::cos(v)) * std::cos(w),
        (bigR + smallR * std::cos(v)) * std::sin(w),
        smallR * std::sin(v));
}

Vector3Dd OpenGL1TorusRenderer::normal(double w, double v)
{
    return Vector3Dd(std::cos(v) * std::cos(w), std::cos(v) * std::sin(w),
                     std::sin(v));
}
