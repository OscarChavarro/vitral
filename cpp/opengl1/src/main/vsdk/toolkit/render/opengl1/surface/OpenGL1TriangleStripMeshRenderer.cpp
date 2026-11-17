#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleStripMeshRenderer.h"

void OpenGL1TriangleStripMeshRenderer::draw(TriangleStripMesh* strips, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(strips, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

java::String OpenGL1TriangleStripMeshRenderer::meshKey(TriangleStripMesh& strips)
{
    char key[64];
    snprintf(key, sizeof(key), "trianglestripmesh/%p", (void*)&strips);
    return key;
}

OpenGL1MeshRenderer::Mesh* OpenGL1TriangleStripMeshRenderer::buildMesh(
    TriangleStripMesh& strips)
{
    TriangleMesh* mesh = strips.exportToTriangleMesh();
    if ( mesh == nullptr || mesh->getNumTriangles() <= 0 ) {
        delete mesh;
        return nullptr;
    }
    java::ArrayList<double>& p = mesh->getVertexPositions();
    java::ArrayList<int>& idx = mesh->getTriangleIndexes();
    double* minmax = strips.getMinMax();
    OpenGL1MeshBuilder builder(OpenGL1MeshBuilder::characteristicSize(minmax));
    delete[] minmax;
    builder.setDoubleSided(true);
    for ( long t = 0; t + 2 < idx.size(); t += 3 ) {
        Vector3Dd pos[3];
        Vector3Dd nor[3];
        for ( int k = 0; k < 3; k++ ) {
            int i = idx[t + k];
            pos[k] = Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
            nor[k] = strips.getVertexAt(i).normal;
        }
        Vector3Dd face = pos[1].subtract(pos[0]).crossProduct(
            pos[2].subtract(pos[0]));
        if ( !(face.length() > 0) ) {
            continue;
        }
        for ( int k = 0; k < 3; k++ ) {
            if ( !(nor[k].length() > 0) ) {
                nor[k] = face;
            }
        }
        builder.addTriangle(pos[0], nor[0], 0, 0, pos[1], nor[1], 0, 0,
            pos[2], nor[2], 0, 0);
    }
    delete mesh;
    return builder.build();
}
