#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.h"

#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

void PolyhedralBoundedSolidTopologyEditing::loopGlue(PolyhedralBoundedSolid*, int) {}

void PolyhedralBoundedSolidTopologyEditing::compactIds(PolyhedralBoundedSolid* solid)
{
    if ( solid == 0 ) return;
    int i;
    std::vector<_PolyhedralBoundedSolidVertex*>& verts = solid->getVerticesList();
    for (i = 0; i < static_cast<int>(verts.size()); ++i) verts[i]->id = i + 1;
    std::vector<_PolyhedralBoundedSolidFace*>& faces = solid->getPolygonsList();
    for (i = 0; i < static_cast<int>(faces.size()); ++i) faces[i]->id = i + 1;
    solid->setMaxVertexId(static_cast<int>(verts.size()));
    solid->setMaxFaceId(static_cast<int>(faces.size()));
}

void PolyhedralBoundedSolidTopologyEditing::maximizeFaces(PolyhedralBoundedSolid*) {}
int PolyhedralBoundedSolidTopologyEditing::weldCoincidentVertices(PolyhedralBoundedSolid*) { return 0; }
