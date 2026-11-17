#include <stddef.h>
#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleStripMeshRenderer.h"

String OpenGL1TriangleStripMeshRenderer::meshKey(TriangleStripMesh& strips)
{
    char buffer[64];
    sprintf(buffer, "trianglestripmesh/%p", (void*)&strips);
    return String(buffer);
}

/**
C++ port note: the exported mesh is deleted here.
*/
OpenGL1MeshRenderer::Mesh* OpenGL1TriangleStripMeshRenderer::buildMesh(
    TriangleStripMesh& strips)
{
    TriangleMesh* mesh = strips.exportToTriangleMesh();
    if ( mesh == NULL || mesh->getNumTriangles() <= 0 ) {
        delete mesh;
        return NULL;
    }
    ArrayList<double>& p = mesh->getVertexPositions();
    ArrayList<int>& idx = mesh->getTriangleIndexes();
    OpenGL1MeshBuilder builder(
        OpenGL1MeshBuilder::characteristicSize(strips.getMinMax()));
    long t;
    int k;

    builder.setDoubleSided(TRUE);
    for ( t = 0; t + 2 < idx.size(); t += 3 ) {
        Vector3Dd pos[3];
        Vector3Dd nor[3];
        for ( k = 0; k < 3; k++ ) {
            long i = idx[t + k];
            pos[k] = Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
            nor[k] = strips.getVertexAt((int)i).getNormal();
        }
        Vector3Dd face = pos[1].subtract(pos[0]).crossProduct(
            pos[2].subtract(pos[0]));
        if ( !(face.length() > 0) ) {
            continue;
        }
        for ( k = 0; k < 3; k++ ) {
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
