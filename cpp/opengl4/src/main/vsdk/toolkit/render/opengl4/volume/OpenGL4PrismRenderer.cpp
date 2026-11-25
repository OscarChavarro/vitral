#include "java/lang/Math.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <cstdarg>
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/Prism.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4PrismRenderer.h"

void OpenGL4PrismRenderer::draw(Prism* prism, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL4GeometryRenderer::draw(prism, camera, lights, material, quality,
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

java::String OpenGL4PrismRenderer::meshKey(const Prism& prism)
{
    const java::ArrayList<Vector2Dd>& base = prism.getBase();
    java::String key = format("prism/%.17g/%ld", prism.getHeight(),
        base.size());
    for ( long int i = 0; i < base.size(); i++ ) {
        key = key + format("/%.17g/%.17g", base[i].x, base[i].y);
    }
    return key;
}

OpenGL4MeshRenderer::Mesh* OpenGL4PrismRenderer::buildMesh(const Prism& prism)
{
    java::ArrayList<int> triangles;
    if ( !prism.hasValidParameters() || prism.triangulateBase(triangles) <= 0 ) {
        return nullptr;
    }
    const java::ArrayList<Vector2Dd>& base = prism.getBase();
    long int n = base.size();
    double h = prism.getHeight();
    double r = prism.getCircumradius();
    OpenGL4MeshBuilder builder(java::Math::max(2 * r, h));

    // Texture coordinates as `Prism::doExtraInformation`: the caps are
    // mapped from the box of the base, the walls around the perimeter
    double minX = base[0].x;
    double minY = base[0].y;
    double maxX = minX;
    double maxY = minY;
    for ( long int i = 1; i < n; i++ ) {
        minX = java::Math::min(minX, base[i].x);
        minY = java::Math::min(minY, base[i].y);
        maxX = java::Math::max(maxX, base[i].x);
        maxY = java::Math::max(maxY, base[i].y);
    }
    double width = maxX - minX;
    double depth = maxY - minY;

    //- Walls: the base is counterclockwise, the exterior on the right -
    for ( long int i = 0; i < n; i++ ) {
        const Vector2Dd& a = base[i];
        const Vector2Dd& b = base[(i + 1) % n];
        double ex = b.x - a.x;
        double ey = b.y - a.y;
        double length = java::Math::sqrt(ex * ex + ey * ey);
        Vector3Dd normal(ey / length, -ex / length, 0);
        double u0 = ((double)i) / ((double)n);
        double u1 = ((double)(i + 1)) / ((double)n);
        builder.addQuad(
            Vector3Dd(a.x, a.y, 0), normal, u0, 0,
            Vector3Dd(b.x, b.y, 0), normal, u1, 0,
            Vector3Dd(b.x, b.y, h), normal, u1, 1,
            Vector3Dd(a.x, a.y, h), normal, u0, 1);
    }

    //- Caps -----------------------------------------------------------
    Vector3Dd up(0, 0, 1);
    Vector3Dd down(0, 0, -1);
    for ( long int i = 0; i < triangles.size(); i += 3 ) {
        const Vector2Dd& a = base[triangles[i]];
        const Vector2Dd& b = base[triangles[i + 1]];
        const Vector2Dd& c = base[triangles[i + 2]];
        double ua = (a.x - minX) / width;
        double va = (a.y - minY) / depth;
        double ub = (b.x - minX) / width;
        double vb = (b.y - minY) / depth;
        double uc = (c.x - minX) / width;
        double vc = (c.y - minY) / depth;
        builder.addTriangle(
            Vector3Dd(a.x, a.y, h), up, ua, va,
            Vector3Dd(b.x, b.y, h), up, ub, vb,
            Vector3Dd(c.x, c.y, h), up, uc, vc);
        builder.addTriangle(
            Vector3Dd(a.x, a.y, 0), down, ua, va,
            Vector3Dd(c.x, c.y, 0), down, uc, vc,
            Vector3Dd(b.x, b.y, 0), down, ub, vb);
    }
    return builder.build();
}
