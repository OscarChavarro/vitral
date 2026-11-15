#include "vsdk/toolkit/common/VSDK.h"
#include <stdio.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/element/Vertex.h"
#include "vsdk/toolkit/environment/geometry/surface/QuadMesh.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/geometry/ReaderMitQuadGroup.h"

ReaderMitQuadGroup::ReaderMitQuadGroup() {}
ReaderMitQuadGroup::~ReaderMitQuadGroup() {}
void ReaderMitQuadGroup::reset() { vertices.clear(); quadIndices.clear(); }
BOOLEAN ReaderMitQuadGroup::addVertex(const Vector3Dd& vertex) { return vertices.add(vertex); }

BOOLEAN ReaderMitQuadGroup::addQuad(const char* indices)
{ int i;
    int q[4];
    char extra[2];
    if (sscanf(indices, "%d %d %d %d %1s", &q[0], &q[1], &q[2], &q[3], extra) != 4) return FALSE;
    for (i = 0; i < 4; ++i)
        if (q[i] < 0 || q[i] >= vertices.size()) return FALSE;
    for (i = 0; i < 4; ++i)
        if (!quadIndices.add(q[i])) return FALSE;
    return TRUE;
}

SimpleBody* ReaderMitQuadGroup::flush(SimpleScene* scene, const SimpleMaterial* material)
{ long int i;
    if (quadIndices.isEmpty()) return NULL;
    ArrayList<Vertex> meshVertices;
    for (i = 0; i < vertices.size(); ++i) meshVertices.add(Vertex(vertices[i]));
    QuadMesh* mesh = new QuadMesh();
    mesh->setVertexes(meshVertices);
    const int quadCount = (int)(quadIndices.size() / 4);
    mesh->initQuadArrays(quadCount);
    ArrayList<int>& target = mesh->getQuadIndices();
    for (i = 0; i < quadIndices.size(); ++i) target[i] = quadIndices[i];
    SimpleBody* body = new SimpleBody();
    body->setGeometry(mesh);
    body->setMaterial(new SimpleMaterial(*material));
    scene->addBody(body);
    quadIndices.clear();
    return body;
}
