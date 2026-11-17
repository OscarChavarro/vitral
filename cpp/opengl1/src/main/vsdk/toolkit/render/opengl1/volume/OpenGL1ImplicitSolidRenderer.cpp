#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <cstdarg>
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobComponent.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/ImplicitSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.h"
#include "vsdk/toolkit/render/opengl1/volume/_OpenGL1SolidBoundaryMeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1ImplicitSolidRenderer.h"

void OpenGL1ImplicitSolidRenderer::draw(ImplicitSolid* solid, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(solid, camera, lights, material, quality,
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

java::String OpenGL1ImplicitSolidRenderer::meshKey(ImplicitSolid& solid)
{
    BlobSolid* b = dynamic_cast<BlobSolid*>(&solid);
    if ( b != nullptr ) {
        java::String key = format("blob/%.17g", b->getThreshold());
        for ( long i = 0; i < b->getComponentCount(); i++ ) {
            const BlobComponent& c = b->getComponent(i);
            key = key + "/" + keyOf(c.getCenter()) +
                format("/%.17g/%.17g", c.getRadius(), c.getStrength());
        }
        return key + format("/%d", _OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
    }
    PolynomialSurface* p = dynamic_cast<PolynomialSurface*>(&solid);
    if ( p != nullptr ) {
        // C++ port note: Java's `Arrays.toString` of every coefficient
        java::String key = format("polynomial/%d/[", p->getOrder());
        for ( int i = 0; i < PolynomialSurface::MAXIMUM_TERMS; i++ ) {
            key = key + format(i > 0 ? ", %.17g" : "%.17g",
                p->getCoefficients()[i]);
        }
        return key + format("]/%d", _OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
    }
    return format("tessellatedsolid/%p/%d", (void*)&solid,
        _OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
}

OpenGL1MeshRenderer::Mesh* OpenGL1ImplicitSolidRenderer::buildMesh(
    ImplicitSolid& solid)
{
    return _OpenGL1SolidBoundaryMeshBuilder::buildMesh(&solid);
}

java::String OpenGL1ImplicitSolidRenderer::keyOf(const Vector3Dd& v)
{
    return format("%.17g,%.17g,%.17g", v.x(), v.y(), v.z());
}
