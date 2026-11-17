#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include <algorithm>
#include <cstdarg>
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4FunctionalExplicitSurfaceRenderer.h"

void OpenGL4FunctionalExplicitSurfaceRenderer::draw(FunctionalExplicitSurface* surface, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL4GeometryRenderer::draw(surface, camera, lights, material, quality,
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
C++ port note: keyed by content, not by address (as Java does, by identity):
editors replace the surface of a body with a new one, which the allocator may
place at the address of the deleted one (and its mesh at the old mesh one).
*/
java::String OpenGL4FunctionalExplicitSurfaceRenderer::meshKey(
    FunctionalExplicitSurface& surface)
{
    return format("functionalexplicitsurface/%.17g/%.17g/%.17g/%.17g/"
        "%.17g/%.17g/%d/%d/", surface.getMinXBound(), surface.getMinYBound(),
        surface.getMinZBound(), surface.getMaxXBound(), surface.getMaxYBound(),
        surface.getMaxZBound(), surface.getTesselationHintX(),
        surface.getTesselationHintY()) + surface.getFunctionExpression().c_str();
}

OpenGL4MeshRenderer::Mesh* OpenGL4FunctionalExplicitSurfaceRenderer::buildMesh(
    FunctionalExplicitSurface& surface)
{
    TriangleMesh* mesh = surface.getInternalTriangleMesh();
    if ( mesh == nullptr ) {
        return nullptr;
    }
    java::ArrayList<double>& p = mesh->getVertexPositions();
    java::ArrayList<double>& n = mesh->getVertexNormals();
    java::ArrayList<int>& idx = mesh->getTriangleIndexes();
    if ( p.size() == 0 || idx.size() < 3 ) {
        return nullptr;
    }
    double sizeX = surface.getMaxXBound() - surface.getMinXBound();
    double sizeY = surface.getMaxYBound() - surface.getMinYBound();
    double sizeZ = surface.getMaxZBound() - surface.getMinZBound();
    OpenGL4MeshBuilder builder(std::max(std::fabs(sizeX),
        std::max(std::fabs(sizeY), std::fabs(sizeZ))));
    Vector3Dd pos[3];
    Vector3Dd nor[3];
    bool hasNormal[3];
    double u[3];
    double v[3];
    builder.setDoubleSided(true);
    for ( long t = 0; t + 2 < idx.size(); t += 3 ) {
        for ( int k = 0; k < 3; k++ ) {
            long i = idx[t + k];
            pos[k] = Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
            hasNormal[k] = n.size() >= 3 * i + 3;
            if ( hasNormal[k] ) {
                nor[k] = Vector3Dd(n[3 * i], n[3 * i + 1], n[3 * i + 2]);
            }
            u[k] = sizeX != 0 ? (pos[k].x() - surface.getMinXBound()) / sizeX : 0;
            v[k] = sizeY != 0 ? (pos[k].y() - surface.getMinYBound()) / sizeY : 0;
        }
        Vector3Dd face = pos[1].subtract(pos[0]).crossProduct(pos[2].subtract(pos[0]));
        if ( face.length() < 1e-15 ) {
            continue;
        }
        for ( int k = 0; k < 3; k++ ) {
            if ( !hasNormal[k] || !(nor[k].length() > 1e-12) ) {
                nor[k] = face;
            }
        }
        // The front side is the one the normals point to
        if ( nor[0].add(nor[1]).add(nor[2]).dotProduct(face) >= 0 ) {
            builder.addTriangle(pos[0], nor[0], u[0], v[0], pos[1], nor[1], u[1], v[1],
                pos[2], nor[2], u[2], v[2]);
        }
        else {
            builder.addTriangle(pos[2], nor[2], u[2], v[2], pos[1], nor[1], u[1], v[1],
                pos[0], nor[0], u[0], v[0]);
        }
    }
    return builder.build();
}
