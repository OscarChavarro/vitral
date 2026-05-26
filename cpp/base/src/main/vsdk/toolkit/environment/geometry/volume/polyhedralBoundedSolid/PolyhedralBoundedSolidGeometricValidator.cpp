#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "java/lang/String.h"

#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "java/lang/String.h"

#include "java/util/ArrayList.txx"
#include "java/lang/String.h"

#include <set>
#include "java/lang/String.h"

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
bool PolyhedralBoundedSolidGeometricValidator::validateAllFacesPlanarityAndPlanes(PolyhedralBoundedSolid*, java::String*) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateConsistentFaceOrientations(PolyhedralBoundedSolid*, java::String*) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateLoopsStrict(PolyhedralBoundedSolid*, java::String*) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateFaceIntersectionsStrict(PolyhedralBoundedSolid*, java::String*) { return true; }
bool PolyhedralBoundedSolidGeometricValidator::validateNoCoincidentVertices(PolyhedralBoundedSolid*, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext&, java::String*) { return true; }

bool PolyhedralBoundedSolidGeometricValidator::validateUniqueFaceAndVertexIds(PolyhedralBoundedSolid* solid, java::String*)
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
