#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <algorithm>
#include <cstdarg>
#include "java/util/Collections.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1ParametricBiCubicPatchRenderer.h"

void OpenGL1ParametricBiCubicPatchRenderer::draw(ParametricBiCubicPatch* patch, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(patch, camera, lights, material, quality,
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

/**
C++ port note: Java keys by the hash codes of the geometry matrices; here a
weighted sum of their elements is used.
*/
java::String OpenGL1ParametricBiCubicPatchRenderer::meshKey(
    ParametricBiCubicPatch& patch)
{
    double hash = 0;
    for ( int i = 0; i < 4; i++ ) {
        for ( int j = 0; j < 4; j++ ) {
            hash += (i * 4 + j + 1) * (patch.geometryMatrixX.get(i, j) +
                3 * patch.geometryMatrixY.get(i, j) +
                7 * patch.geometryMatrixZ.get(i, j));
        }
    }
    return format("parametricbicubicpatch/%p/%d/%d/%.17g", (void*)&patch,
        patch.getType(), patch.getApproximationSteps(), hash);
}

OpenGL1MeshRenderer::Mesh* OpenGL1ParametricBiCubicPatchRenderer::buildMesh(
    ParametricBiCubicPatch& patch)
{
    int steps = std::max(1, patch.getApproximationSteps());
    int n = steps + 1;
    java::ArrayList<Vector3Dd> points =
        java::Collections::nCopies<Vector3Dd>((size_t)n * n, Vector3Dd());
    java::ArrayList<Vector3Dd> normals =
        java::Collections::nCopies<Vector3Dd>((size_t)n * n, Vector3Dd());

    for ( int i = 0; i < n; i++ ) {
        double s = (double)i / steps;
        for ( int j = 0; j < n; j++ ) {
            double t = (double)j / steps;
            patch.evaluate(points[(size_t)i * n + j], s, t);
            normals[(size_t)i * n + j] = patch.evaluateNormal(s, t);
        }
    }
    double* minmax = patch.getMinMax();
    double size = 1.0;
    if ( minmax != nullptr ) {
        size = std::max(std::fabs(minmax[3] - minmax[0]),
            std::max(std::fabs(minmax[4] - minmax[1]),
                     std::fabs(minmax[5] - minmax[2])));
        delete[] minmax;
    }
    OpenGL1MeshBuilder builder(size);
    builder.setDoubleSided(true);
    for ( int i = 0; i < steps; i++ ) {
        for ( int j = 0; j < steps; j++ ) {
            addCell(builder, points, normals, i, j, steps);
        }
    }
    return builder.build();
}

void OpenGL1ParametricBiCubicPatchRenderer::addCell(
    OpenGL1MeshBuilder& builder, const java::ArrayList<Vector3Dd>& points,
    const java::ArrayList<Vector3Dd>& normals, int i, int j, int steps)
{
    size_t n = (size_t)steps + 1;
    Vector3Dd p00 = points[i * n + j];
    Vector3Dd p10 = points[(i + 1) * n + j];
    Vector3Dd p11 = points[(i + 1) * n + j + 1];
    Vector3Dd p01 = points[i * n + j + 1];
    Vector3Dd face = p11.subtract(p00).crossProduct(p01.subtract(p10));
    if ( !(face.length() > 1e-15) ) {
        return;
    }
    Vector3Dd n00 = validNormal(normals[i * n + j], face);
    Vector3Dd n10 = validNormal(normals[(i + 1) * n + j], face);
    Vector3Dd n11 = validNormal(normals[(i + 1) * n + j + 1], face);
    Vector3Dd n01 = validNormal(normals[i * n + j + 1], face);
    double u0 = (double)i / steps;
    double u1 = (double)(i + 1) / steps;
    double v0 = (double)j / steps;
    double v1 = (double)(j + 1) / steps;
    Vector3Dd average = n00.add(n10).add(n11).add(n01);
    if ( average.dotProduct(face) >= 0 ) {
        builder.addQuad(p00, n00, u0, v0, p10, n10, u1, v0,
            p11, n11, u1, v1, p01, n01, u0, v1);
    }
    else {
        builder.addQuad(p01, n01, u0, v1, p11, n11, u1, v1,
            p10, n10, u1, v0, p00, n00, u0, v0);
    }
}

Vector3Dd OpenGL1ParametricBiCubicPatchRenderer::validNormal(
    const Vector3Dd& normal, const Vector3Dd& fallback)
{
    if ( !(normal.length() > 1e-12) ) {
        return fallback.normalized();
    }
    return normal;
}
