#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <algorithm>
#include <cstdarg>
#include <limits>
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4InfinitePlaneRenderer.h"

void OpenGL4InfinitePlaneRenderer::draw(InfinitePlane* plane, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL4GeometryRenderer::draw(plane, camera, lights, material, quality,
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

void OpenGL4InfinitePlaneRenderer::drawMesh(OpenGL4MeshRenderer::Mesh* mesh,
    InfinitePlane* plane, Camera* camera, const java::ArrayList<Light*>* lights,
    const SimpleMaterial* material, const RendererConfiguration* quality,
    RGBImageUncompressed* textureMap, RGBImageUncompressed* normalMap,
    const Matrix4x4d& localTransform)
{
    RendererConfiguration meshQuality = quality->clone();

    meshQuality.setBoundingVolume(false);
    meshQuality.setSelectionCorners(false);
    OpenGL4MeshRenderer::draw(mesh, plane, camera, lights, material,
        &meshQuality, textureMap, normalMap, localTransform);

    double shown[6];
    calculateShownMinMax(*plane, shown);
    if ( quality->isBoundingVolumeSet() ) {
        OpenGL4MinMaxRenderer::draw(shown, camera, localTransform);
    }
    if ( quality->isSelectionCornersSet() ) {
        OpenGL4SelectionCornersRenderer::draw(shown, camera, localTransform);
    }
}

java::String OpenGL4InfinitePlaneRenderer::meshKey(const InfinitePlane& plane)
{
    return format("infiniteplane/%.17g/%.17g/%.17g/%.17g", plane.getA(),
        plane.getB(), plane.getC(), plane.getD());
}

void OpenGL4InfinitePlaneRenderer::calculateShownMinMax(
    const InfinitePlane& plane, double outMinMax[6])
{
    Vector3Dd frame[4];
    calculateFrame(plane, frame);
    for ( int k = 0; k < 3; k++ ) {
        outMinMax[k] = std::numeric_limits<double>::max();
        outMinMax[k + 3] = -std::numeric_limits<double>::max();
    }
    double h = SIZE / 2;
    for ( int i = -1; i <= 1; i += 2 ) {
        for ( int j = -1; j <= 1; j += 2 ) {
            Vector3Dd p = frame[0].add(frame[1].multiply(i * h))
                .add(frame[2].multiply(j * h));
            double c[3] = { p.x(), p.y(), p.z() };
            for ( int k = 0; k < 3; k++ ) {
                outMinMax[k] = std::min(outMinMax[k], c[k]);
                outMinMax[k + 3] = std::max(outMinMax[k + 3], c[k]);
            }
        }
    }
}

OpenGL4MeshRenderer::Mesh* OpenGL4InfinitePlaneRenderer::buildMesh(
    const InfinitePlane& plane)
{
    Vector3Dd frame[4];
    calculateFrame(plane, frame);
    Vector3Dd center = frame[0];
    Vector3Dd u = frame[1];
    Vector3Dd v = frame[2];
    Vector3Dd n = frame[3];
    double h = SIZE / 2;
    double d = SIZE / TILES;
    OpenGL4MeshBuilder builder(SIZE);
    builder.setDoubleSided(true);

    for ( int i = 0; i < TILES; i++ ) {
        double a0 = -h + i * d;
        double a1 = a0 + d;
        for ( int j = 0; j < TILES; j++ ) {
            double b0 = -h + j * d;
            double b1 = b0 + d;
            // (u, v, n) is right handed: counterclockwise seen from n
            builder.addQuad(
                center.add(u.multiply(a0)).add(v.multiply(b0)), n,
                (double)i / TILES, (double)j / TILES,
                center.add(u.multiply(a1)).add(v.multiply(b0)), n,
                (double)(i + 1) / TILES, (double)j / TILES,
                center.add(u.multiply(a1)).add(v.multiply(b1)), n,
                (double)(i + 1) / TILES, (double)(j + 1) / TILES,
                center.add(u.multiply(a0)).add(v.multiply(b1)), n,
                (double)i / TILES, (double)(j + 1) / TILES);
        }
    }
    return builder.build();
}

void OpenGL4InfinitePlaneRenderer::calculateFrame(const InfinitePlane& plane,
                                                  Vector3Dd outFrame[4])
{
    Vector3Dd n = plane.getNormal();
    double length = std::sqrt(plane.getA() * plane.getA() +
        plane.getB() * plane.getB() + plane.getC() * plane.getC());
    // The plane is a x + b y + c z + d = 0; (a, b, c) may not be unitary
    Vector3Dd center = n.multiply(length > 0 ? -plane.getD() / length : 0);
    Vector3Dd reference = std::fabs(n.z()) < 0.9 ?
        Vector3Dd(0, 0, 1) : Vector3Dd(1, 0, 0);
    Vector3Dd u = reference.crossProduct(n).normalized();
    Vector3Dd v = n.crossProduct(u).normalized();

    outFrame[0] = center;
    outFrame[1] = u;
    outFrame[2] = v;
    outFrame[3] = n;
}
