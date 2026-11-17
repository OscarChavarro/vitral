#include <stdio.h>
#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1TubeRenderer.h"

String OpenGL1TubeRenderer::meshKey(const Tube& tube)
{
    char buffer[512];
    sprintf(buffer, "tube/%.17g/%.17g/%.17g", tube.getInternalRadius(),
        tube.getExternalRadius(), tube.getHeight());
    return String(buffer);
}

OpenGL1MeshRenderer::Mesh* OpenGL1TubeRenderer::buildMesh(const Tube& tube)
{
    double ri = tube.getInternalRadius();
    double re = tube.getExternalRadius();
    double h = tube.getHeight();
    OpenGL1MeshBuilder builder(Math::max(re, h));

    if ( tube.isWasher() ) {
        builder.addDisk(0, ri, re, TRUE, SLICES);
        builder.setDoubleSided(TRUE);
        return builder.build();
    }
    builder.addFrustum(0, re, h, re, SLICES);
    if ( ri > 0.0 ) {
        addInternalWall(builder, ri, h);
    }
    builder.addDisk(0, ri, re, FALSE, SLICES);
    builder.addDisk(h, ri, re, TRUE, SLICES);
    return builder.build();
}

void OpenGL1TubeRenderer::addInternalWall(OpenGL1MeshBuilder& builder,
    double r, double h)
{
    for ( int i = 0; i < SLICES; i++ ) {
        double a0 = 2 * Math::PI * i / SLICES;
        double a1 = 2 * Math::PI * (i + 1) / SLICES;
        double c0 = Math::cos(a0);
        double s0 = Math::sin(a0);
        double c1 = Math::cos(a1);
        double s1 = Math::sin(a1);
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
