#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobComponent.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/ImplicitSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1ImplicitSolidRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/_OpenGL1SolidBoundaryMeshBuilder.h"

String OpenGL1ImplicitSolidRenderer::meshKey(ImplicitSolid& solid)
{
    char buffer[160];
    long i;

    if ( solid.instanceOf(BlobSolid::CLASS) ) {
        BlobSolid& b = (BlobSolid&)solid;
        sprintf(buffer, "blob/%.17g", b.getThreshold());
        String key(buffer);
        for ( i = 0; i < b.getComponentCount(); i++ ) {
            const BlobComponent& c = b.getComponent(i);
            sprintf(buffer, "/%.17g,%.17g,%.17g/%.17g/%.17g", c.getCenter().x(),
                c.getCenter().y(), c.getCenter().z(), c.getRadius(),
                c.getStrength());
            key = key + buffer;
        }
        sprintf(buffer, "/%d", (int)_OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
        return key + buffer;
    }
    if ( solid.instanceOf(PolynomialSurface::CLASS) ) {
        PolynomialSurface& p = (PolynomialSurface&)solid;
        int terms = PolynomialSurface::getTermCount(p.getOrder());
        sprintf(buffer, "polynomial/%d/", p.getOrder());
        String key(buffer);
        for ( i = 0; i < terms; i++ ) {
            sprintf(buffer, "%s%.17g", i > 0 ? "," : "", p.getCoefficients()[i]);
            key = key + buffer;
        }
        sprintf(buffer, "/%d", (int)_OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
        return key + buffer;
    }
    sprintf(buffer, "tessellatedsolid/%p/%d", (void*)&solid,
        (int)_OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
    return String(buffer);
}

OpenGL1MeshRenderer::Mesh* OpenGL1ImplicitSolidRenderer::buildMesh(
    ImplicitSolid& solid)
{
    return _OpenGL1SolidBoundaryMeshBuilder::buildMesh(&solid);
}
