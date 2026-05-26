#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"

#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

#include "java/util/ArrayList.txx"

#include <set>

bool PolyhedralBoundedSolidGeometricValidator::validateFacePointsAreCoplanar(java::ArrayList<Vector3Dd>&) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateFacePointsAreCoplanar(java::ArrayList<Vector3Dd>&, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext&) { return true; }

void PolyhedralBoundedSolidGeometricValidator::extractPointsFromFace(_PolyhedralBoundedSolidFace* face, java::ArrayList<Vector3Dd>& outPoints)
{
    if ( face == 0 || face->boundariesList.size() == 0 ) return;
    _PolyhedralBoundedSolidLoop* l = face->boundariesList[0];
    for (long int i = 0; i < l->halfEdgesList.size(); ++i) {
        if ( l->halfEdgesList[i] && l->halfEdgesList[i]->startingVertex ) outPoints.add(l->halfEdgesList[i]->startingVertex->position);
    }
}

bool PolyhedralBoundedSolidGeometricValidator::validateFaceIsPlanar(_PolyhedralBoundedSolidFace*) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateFaceIsPlanar(_PolyhedralBoundedSolidFace*, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext&) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateAllFacesPlanarityAndPlanes(PolyhedralBoundedSolid*, std::string*) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateConsistentFaceOrientations(PolyhedralBoundedSolid*, std::string*) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateLoopsStrict(PolyhedralBoundedSolid*, std::string*) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateFaceIntersectionsStrict(PolyhedralBoundedSolid*, std::string*) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateNoCoincidentVertices(PolyhedralBoundedSolid*, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext&, std::string*) { return true; }

bool PolyhedralBoundedSolidGeometricValidator::validateUniqueFaceAndVertexIds(PolyhedralBoundedSolid* solid, std::string*)
{
    if ( solid == 0 ) return false;
    std::set<int> faceIds;
    std::set<int> vertexIds;
    java::ArrayList<_PolyhedralBoundedSolidFace*>& faces = solid->getPolygonsList();
    java::ArrayList<_PolyhedralBoundedSolidVertex*>& verts = solid->getVerticesList();
    for (long int i = 0; i < faces.size(); ++i) {
        if ( !faceIds.insert(faces[i]->id).second ) return false;
    }
    for (long int i = 0; i < verts.size(); ++i) {
        if ( !vertexIds.insert(verts[i]->id).second ) return false;
    }
    return true;
}
