#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <algorithm>
#include <cstdarg>
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1BoxRenderer.h"

void OpenGL1BoxRenderer::draw(Box* box, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(box, camera, lights, material, quality,
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

java::String OpenGL1BoxRenderer::meshKey(const Box& box)
{
    Vector3Dd s = box.getSize();
    return format("box/%.17g/%.17g/%.17g", s.x(), s.y(), s.z());
}

OpenGL1MeshRenderer::Mesh* OpenGL1BoxRenderer::buildMesh(const Box& box)
{
    Vector3Dd size = box.getSize();
    double hx = size.x() / 2;
    double hy = size.y() / 2;
    double hz = size.z() / 2;
    OpenGL1MeshBuilder builder(std::max(hx, std::max(hy, hz)) * 2);

    // Each face is given counterclockwise seen from outside
    addFace(builder, Vector3Dd(0, 0, -1),
        Vector3Dd(-hx, -hy, -hz), Vector3Dd(-hx, hy, -hz),
        Vector3Dd(hx, hy, -hz), Vector3Dd(hx, -hy, -hz));
    addFace(builder, Vector3Dd(0, 0, 1),
        Vector3Dd(-hx, -hy, hz), Vector3Dd(hx, -hy, hz),
        Vector3Dd(hx, hy, hz), Vector3Dd(-hx, hy, hz));
    addFace(builder, Vector3Dd(0, -1, 0),
        Vector3Dd(-hx, -hy, hz), Vector3Dd(-hx, -hy, -hz),
        Vector3Dd(hx, -hy, -hz), Vector3Dd(hx, -hy, hz));
    addFace(builder, Vector3Dd(-1, 0, 0),
        Vector3Dd(-hx, hy, hz), Vector3Dd(-hx, hy, -hz),
        Vector3Dd(-hx, -hy, -hz), Vector3Dd(-hx, -hy, hz));
    addFace(builder, Vector3Dd(0, 1, 0),
        Vector3Dd(hx, hy, hz), Vector3Dd(hx, hy, -hz),
        Vector3Dd(-hx, hy, -hz), Vector3Dd(-hx, hy, hz));
    addFace(builder, Vector3Dd(1, 0, 0),
        Vector3Dd(hx, -hy, hz), Vector3Dd(hx, -hy, -hz),
        Vector3Dd(hx, hy, -hz), Vector3Dd(hx, hy, hz));
    return builder.build();
}

void OpenGL1BoxRenderer::addFace(OpenGL1MeshBuilder& builder,
    const Vector3Dd& n, const Vector3Dd& a, const Vector3Dd& b,
    const Vector3Dd& c, const Vector3Dd& d)
{
    Vector3Dd winding = b.subtract(a).crossProduct(c.subtract(b));

    if ( winding.dotProduct(n) >= 0 ) {
        builder.addQuad(a, n, 0, 1, b, n, 0, 0, c, n, 1, 0, d, n, 1, 1);
    }
    else {
        builder.addQuad(d, n, 1, 1, c, n, 1, 0, b, n, 0, 0, a, n, 0, 1);
    }
}
