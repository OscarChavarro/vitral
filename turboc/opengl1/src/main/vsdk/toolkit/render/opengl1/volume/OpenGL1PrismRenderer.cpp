#include <stdio.h>
#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Math.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/Prism.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1PrismRenderer.h"

void OpenGL1PrismRenderer::draw(Prism* prism, Camera* camera,
    const ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(prism, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

// C++ port note: the key is formatted with sprintf (no variadic format
// helper in an anonymous namespace, as in OpenGL1TubeRenderer)
String OpenGL1PrismRenderer::meshKey(const Prism& prism)
{
    const ArrayList<Vector2Dd>& base = prism.getBase();
    char buffer[512];
    long int i;

    sprintf(buffer, "prism/%.17g/%ld", prism.getHeight(), (long)base.size());
    String key(buffer);
    for ( i = 0; i < base.size(); i++ ) {
        sprintf(buffer, "/%.17g/%.17g", base[i].x, base[i].y);
        key = key + buffer;
    }
    return key;
}

OpenGL1MeshRenderer::Mesh* OpenGL1PrismRenderer::buildMesh(const Prism& prism)
{
    ArrayList<int> triangles;
    long int i;

    if ( !prism.hasValidParameters() || prism.triangulateBase(triangles) <= 0 ) {
        return NULL;
    }
    const ArrayList<Vector2Dd>& base = prism.getBase();
    long int n = base.size();
    double h = prism.getHeight();
    double r = prism.getCircumradius();
    OpenGL1MeshBuilder builder(Math::max(2 * r, h));

    // Texture coordinates as `Prism::doExtraInformation`: the caps are
    // mapped from the box of the base, the walls around the perimeter
    double minX = base[0].x;
    double minY = base[0].y;
    double maxX = minX;
    double maxY = minY;
    for ( i = 1; i < n; i++ ) {
        minX = Math::min(minX, base[i].x);
        minY = Math::min(minY, base[i].y);
        maxX = Math::max(maxX, base[i].x);
        maxY = Math::max(maxY, base[i].y);
    }
    double width = maxX - minX;
    double depth = maxY - minY;

    //- Walls: the base is counterclockwise, the exterior on the right -
    for ( i = 0; i < n; i++ ) {
        const Vector2Dd& a = base[i];
        const Vector2Dd& b = base[(i + 1) % n];
        double ex = b.x - a.x;
        double ey = b.y - a.y;
        double length = Math::sqrt(ex * ex + ey * ey);
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
    for ( i = 0; i < triangles.size(); i += 3 ) {
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
