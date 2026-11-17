#include <stddef.h>
#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshRenderer.h"

String OpenGL1TriangleMeshRenderer::meshKey(TriangleMesh& mesh)
{
    char buffer[96];
    sprintf(buffer, "trianglemesh/%p/%d/%d", (void*)&mesh,
        mesh.getNumVertices(), mesh.getNumTriangles());
    return String(buffer);
}

void OpenGL1TriangleMeshRenderer::addTriangles(OpenGL1MeshBuilder& builder,
    TriangleMesh& mesh)
{
    ArrayList<double>& p = mesh.getVertexPositions();
    ArrayList<double>& n = mesh.getVertexNormals();
    ArrayList<double>& uv = mesh.getVertexUvs();
    ArrayList<int>& idx = mesh.getTriangleIndexes();
    long t;
    int k;

    for ( t = 0; t + 2 < idx.size(); t += 3 ) {
        Vector3Dd pos[3];
        Vector3Dd nor[3];
        double u[3] = { 0, 0, 0 };
        double v[3] = { 0, 0, 0 };
        for ( k = 0; k < 3; k++ ) {
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

/**
C++ port note: triangle meshes of the port have no double sided flag, so
they are drawn single sided, as before.
*/
OpenGL1MeshRenderer::Mesh* OpenGL1TriangleMeshRenderer::buildMesh(TriangleMesh& mesh)
{
    return buildMesh(mesh, FALSE);
}

OpenGL1MeshRenderer::Mesh* OpenGL1TriangleMeshRenderer::buildMesh(TriangleMesh& mesh,
    BOOLEAN doubleSided)
{
    if ( mesh.getVertexPositions().size() == 0 ||
         mesh.getTriangleIndexes().size() < 3 ) {
        return NULL;
    }
    OpenGL1MeshBuilder builder(OpenGL1MeshBuilder::characteristicSize(mesh.getMinMax()));
    builder.setDoubleSided(doubleSided);
    addTriangles(builder, mesh);
    return builder.build();
}
