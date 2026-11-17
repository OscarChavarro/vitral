#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshRenderer.h"

void OpenGL1TriangleMeshRenderer::draw(TriangleMesh* mesh, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(mesh, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

java::String OpenGL1TriangleMeshRenderer::meshKey(TriangleMesh& mesh)
{
    // C++ port note: also keyed by its sizes, as a mesh may be replaced by
    // another one at the same address
    char key[96];
    snprintf(key, sizeof(key), "trianglemesh/%p/%d/%d", (void*)&mesh,
             mesh.getNumVertices(), mesh.getNumTriangles());
    return key;
}

/**
C++ port note: `TriangleMesh` has no double sided attribute in C++ yet, so
the meshes are single sided, as Java's default.
*/
OpenGL1MeshRenderer::Mesh* OpenGL1TriangleMeshRenderer::buildMesh(
    TriangleMesh& mesh)
{
    return buildMesh(mesh, false);
}

OpenGL1MeshRenderer::Mesh* OpenGL1TriangleMeshRenderer::buildMesh(
    TriangleMesh& mesh, bool doubleSided)
{
    if ( mesh.getVertexPositions().size() == 0 ||
         mesh.getTriangleIndexes().size() == 0 ) {
        return nullptr;
    }
    double* minmax = mesh.getMinMax();
    OpenGL1MeshBuilder builder(OpenGL1MeshBuilder::characteristicSize(minmax));
    delete[] minmax;
    builder.setDoubleSided(doubleSided);
    addTriangles(builder, mesh);
    return builder.build();
}

void OpenGL1TriangleMeshRenderer::addTriangles(OpenGL1MeshBuilder& builder,
                                               TriangleMesh& mesh)
{
    java::ArrayList<double>& p = mesh.getVertexPositions();
    java::ArrayList<double>& n = mesh.getVertexNormals();
    java::ArrayList<double>& uv = mesh.getVertexUvs();
    java::ArrayList<int>& idx = mesh.getTriangleIndexes();

    for ( long t = 0; t + 2 < idx.size(); t += 3 ) {
        Vector3Dd pos[3];
        Vector3Dd nor[3];
        double u[3] = { 0, 0, 0 };
        double v[3] = { 0, 0, 0 };
        for ( int k = 0; k < 3; k++ ) {
            long i = idx[t + k];
            pos[k] = Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
            nor[k] = n.size() >= 3 * i + 3 ?
                Vector3Dd(n[3 * i], n[3 * i + 1], n[3 * i + 2]) :
                Vector3Dd(0, 0, 1);
            if ( uv.size() >= 2 * i + 2 ) {
                u[k] = uv[2 * i];
                v[k] = uv[2 * i + 1];
            }
        }
        builder.addTriangle(pos[0], nor[0], u[0], v[0], pos[1], nor[1], u[1], v[1],
            pos[2], nor[2], u[2], v[2]);
    }
}
