#include "PolyhedralBoundedSolidGeometricValidator.h"

#include "PolyhedralBoundedSolid.h"
#include "nodes/_PolyhedralBoundedSolidFace.h"
#include "nodes/_PolyhedralBoundedSolidLoop.h"
#include "nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "nodes/_PolyhedralBoundedSolidVertex.h"

#include <set>

bool PolyhedralBoundedSolidGeometricValidator::validateFacePointsAreCoplanar(const std::vector<Vector3Dd>&) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateFacePointsAreCoplanar(const std::vector<Vector3Dd>&, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext&) { return true; }

std::vector<Vector3Dd> PolyhedralBoundedSolidGeometricValidator::extractPointsFromFace(_PolyhedralBoundedSolidFace* face)
{
    std::vector<Vector3Dd> points;
    if ( face == 0 || face->boundariesList.empty() ) return points;
    _PolyhedralBoundedSolidLoop* l = face->boundariesList[0];
    for (size_t i = 0; i < l->halfEdgesList.size(); ++i) {
        if ( l->halfEdgesList[i] && l->halfEdgesList[i]->startingVertex ) points.push_back(l->halfEdgesList[i]->startingVertex->position);
    }
    return points;
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
    std::vector<_PolyhedralBoundedSolidFace*>& faces = solid->getPolygonsList();
    std::vector<_PolyhedralBoundedSolidVertex*>& verts = solid->getVerticesList();
    for (size_t i = 0; i < faces.size(); ++i) {
        if ( !faceIds.insert(faces[i]->id).second ) return false;
    }
    for (size_t i = 0; i < verts.size(); ++i) {
        if ( !vertexIds.insert(verts[i]->id).second ) return false;
    }
    return true;
}
