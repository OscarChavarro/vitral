#include "vsdk/toolkit/common/VSDK.h"
#include <stdlib.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/geometry/ReaderMitTriangleGroup.h"

ReaderMitTriangleGroup::ReaderMitTriangleGroup() {}
ReaderMitTriangleGroup::~ReaderMitTriangleGroup() {}
void ReaderMitTriangleGroup::reset() { vertices.clear(); triangles.clear(); }
BOOLEAN ReaderMitTriangleGroup::addVertex(const Vector3Dd& vertex) { return vertices.add(vertex); }

BOOLEAN ReaderMitTriangleGroup::addFace(const char* coordinates)
{
    ArrayList<int> indices;
    const char* cursor = coordinates;
    while (*cursor != '\0') {
        while (*cursor == ' ' || *cursor == '\t' || *cursor == '\r' || *cursor == '\n') ++cursor;
        if (*cursor == '\0') break;
        char* end = NULL;
        const long value = strtol(cursor, &end, 10);
        if (end == cursor || value < 0 || value >= vertices.size()) return FALSE;
        if (!indices.add((int)value)) return FALSE;
        cursor = end;
    }
    if (indices.size() < 3) return FALSE;
    for (long int i = 2; i < indices.size(); ++i) {
        const int a = indices[0], b = indices[i-1], c = indices[i];
        if (a == b || b == c || a == c) continue;
        if (!triangles.add(Triangle(a,b,c))) return FALSE;
    }
    return TRUE;
}

SimpleBody* ReaderMitTriangleGroup::flush(SimpleScene* scene, const SimpleMaterial* material)
{
    if (triangles.isEmpty()) return NULL;
    TriangleMesh* mesh = new TriangleMesh();
    mesh->initVertexPositionsArray((int)vertices.size());
    ArrayList<double>& positions = mesh->getVertexPositions();
    for (long int i = 0; i < vertices.size(); ++i) {
        positions[i*3] = vertices[i].x();
        positions[i*3+1] = vertices[i].y();
        positions[i*3+2] = vertices[i].z();
    }
    mesh->setTriangles(triangles);
    // Prepare shared read-only normals before parallel ray tracing starts.
    mesh->calculateNormals();
    SimpleBody* body = new SimpleBody();
    body->setGeometry(mesh);
    body->setMaterial(new SimpleMaterial(*material));
    scene->addBody(body);
    triangles.clear();
    return body;
}
