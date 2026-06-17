#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
void PolyhedralBoundedSolidTopologyEditing::loopGlue(PolyhedralBoundedSolid*, int) {}

void PolyhedralBoundedSolidTopologyEditing::compactIds(PolyhedralBoundedSolid* solid)
{
    if ( solid == 0 ) return;
    long int i;
    java::ArrayList<_PolyhedralBoundedSolidVertex*>& verts = solid->getVerticesList();
    for (i = 0; i < verts.size(); ++i) verts[i]->id = static_cast<int>(i + 1);
    java::ArrayList<_PolyhedralBoundedSolidFace*>& faces = solid->getPolygonsList();
    for (i = 0; i < faces.size(); ++i) faces[i]->id = static_cast<int>(i + 1);
    solid->setMaxVertexId(static_cast<int>(verts.size()));
    solid->setMaxFaceId(static_cast<int>(faces.size()));
}

void PolyhedralBoundedSolidTopologyEditing::maximizeFaces(PolyhedralBoundedSolid*) {}
int PolyhedralBoundedSolidTopologyEditing::weldCoincidentVertices(PolyhedralBoundedSolid*) { return 0; }
