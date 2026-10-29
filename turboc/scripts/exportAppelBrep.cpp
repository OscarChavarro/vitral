// Host reference/export tool. Build against the portable base library.
// This preserves the actual Appel fixture's loops, including holes.
#include <stdio.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"

int main(int argc, char** argv)
{
    if (argc != 2) { fprintf(stderr, "Usage: exportAppelBrep output.brep\n"); return 2; }
    PolyhedralBoundedSolid* solid = SimpleTestGeometryLibrary::createTestObjectAPPE1967_3();
    if (!solid) return 1;
    FILE* file = fopen(argv[1], "w");
    if (!file) { delete solid; return 1; }
    ArrayList<_PolyhedralBoundedSolidVertex*>& vertices = solid->getVerticesList();
    ArrayList<_PolyhedralBoundedSolidFace*>& faces = solid->getPolygonsList();
    fprintf(file, "VITRAL_BREP 1\n%ld %ld\n", vertices.size(), faces.size());
    for (long i = 0; i < vertices.size(); ++i) {
        _PolyhedralBoundedSolidVertex* v = vertices[i];
        fprintf(file, "%d %.17g %.17g %.17g\n", v->id,
                v->position.x(), v->position.y(), v->position.z());
    }
    for (long f = 0; f < faces.size(); ++f) {
        _PolyhedralBoundedSolidFace* face = faces[f];
        fprintf(file, "%d %ld\n", face->id, face->boundariesList.size());
        for (long l = 0; l < face->boundariesList.size(); ++l) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList[l];
            fprintf(file, "%ld", loop->halfEdgesList.size());
            _PolyhedralBoundedSolidHalfEdge* edge = loop->boundaryStartHalfEdge;
            for (long h = 0; h < loop->halfEdgesList.size(); ++h) {
                fprintf(file, " %d", edge->startingVertex->id);
                edge = edge->next();
            }
            fputc('\n', file);
        }
    }
    fprintf(stderr, "Appel 3: %ld vertices, %ld edges, %ld faces\n", vertices.size(),
            solid->getEdgesList().size(), faces.size());
    int result = ferror(file);
    if (fclose(file) != 0) result = 1;
    delete solid;
    return result;
}
