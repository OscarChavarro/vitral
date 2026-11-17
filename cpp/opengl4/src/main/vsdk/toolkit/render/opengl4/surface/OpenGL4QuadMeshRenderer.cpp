#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/QuadMesh.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4QuadMeshRenderer.h"

void OpenGL4QuadMeshRenderer::draw(QuadMesh* mesh, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL4GeometryRenderer::draw(mesh, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

java::String OpenGL4QuadMeshRenderer::meshKey(QuadMesh& mesh)
{
    char key[64];
    snprintf(key, sizeof(key), "quadmesh/%p", (void*)&mesh);
    return key;
}

OpenGL4MeshRenderer::Mesh* OpenGL4QuadMeshRenderer::buildMesh(QuadMesh& mesh)
{
    java::ArrayList<double>& p = mesh.getVertexPositions();
    java::ArrayList<double>& n = mesh.getVertexNormals();
    java::ArrayList<double>& uv = mesh.getVertexUvs();
    java::ArrayList<int>& idx = mesh.getQuadIndices();
    long numVertices = p.size() / 3;

    if ( p.size() == 0 || idx.size() == 0 || mesh.getNumQuads() <= 0 ) {
        return nullptr;
    }
    double* minmax = mesh.getMinMax();
    OpenGL4MeshBuilder builder(OpenGL4MeshBuilder::characteristicSize(minmax));
    delete[] minmax;
    builder.setDoubleSided(true);
    for ( long q = 0; q < mesh.getNumQuads() && 4 * q + 3 < idx.size(); q++ ) {
        Vector3Dd pos[4];
        Vector3Dd nor[4];
        bool hasNormal[4] = { false, false, false, false };
        double u[4] = { 0, 0, 0, 0 };
        double v[4] = { 0, 0, 0, 0 };
        bool valid = true;
        for ( int k = 0; k < 4; k++ ) {
            long i = idx[4 * q + k];
            if ( i < 0 || i >= numVertices ) {
                valid = false;
                break;
            }
            pos[k] = Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
            hasNormal[k] = n.size() >= 3 * i + 3;
            if ( hasNormal[k] ) {
                nor[k] = Vector3Dd(n[3 * i], n[3 * i + 1], n[3 * i + 2]);
            }
            if ( uv.size() >= 2 * i + 2 ) {
                u[k] = uv[2 * i];
                v[k] = uv[2 * i + 1];
            }
        }
        if ( !valid ) {
            continue;
        }
        // Normal of the quad: cross product of its diagonals
        Vector3Dd face = pos[2].subtract(pos[0]).crossProduct(
            pos[3].subtract(pos[1]));
        if ( !(face.length() > 0) ) {
            face = Vector3Dd(0, 0, 1);
        }
        for ( int k = 0; k < 4; k++ ) {
            if ( !hasNormal[k] || !(nor[k].length() > 0) ) {
                nor[k] = face;
            }
        }
        builder.addQuad(pos[0], nor[0], u[0], v[0], pos[1], nor[1], u[1], v[1],
            pos[2], nor[2], u[2], v[2], pos[3], nor[3], u[3], v[3]);
    }
    return builder.build();
}
