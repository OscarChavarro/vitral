#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"

#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

#include "java/util/ArrayList.txx"

void PolyhedralBoundedSolidEulerOperators::mvfs(PolyhedralBoundedSolid* solid, const Vector3Dd& pos, int vertexId, int faceId)
{
    if ( solid == 0 ) return;
    _PolyhedralBoundedSolidFace* newFace = new _PolyhedralBoundedSolidFace(solid, faceId);
    _PolyhedralBoundedSolidLoop* newLoop = new _PolyhedralBoundedSolidLoop(newFace);
    _PolyhedralBoundedSolidVertex* newVertex = new _PolyhedralBoundedSolidVertex(pos, vertexId);
    _PolyhedralBoundedSolidHalfEdge* newHalfEdge = new _PolyhedralBoundedSolidHalfEdge(newVertex, newLoop);
    newLoop->halfEdgesList.add(newHalfEdge);
    newLoop->boundaryStartHalfEdge = newHalfEdge;

    solid->getPolygonsList().add(newFace);
    solid->getVerticesList().add(newVertex);
    if ( vertexId > solid->getMaxVertexId() ) solid->setMaxVertexId(vertexId);
    if ( faceId > solid->getMaxFaceId() ) solid->setMaxFaceId(faceId);
}

void PolyhedralBoundedSolidEulerOperators::kvfs(PolyhedralBoundedSolid* solid)
{
    if ( solid == 0 ) return;
    solid->getPolygonsList().clear();
    solid->getEdgesList().clear();
    solid->getVerticesList().clear();
}

void PolyhedralBoundedSolidEulerOperators::lmev(PolyhedralBoundedSolid* solid, _PolyhedralBoundedSolidHalfEdge*, _PolyhedralBoundedSolidHalfEdge*, int vertexId, const Vector3Dd& p)
{
    if ( solid == 0 ) return;
    solid->getVerticesList().add(new _PolyhedralBoundedSolidVertex(p, vertexId));
    if ( vertexId > solid->getMaxVertexId() ) solid->setMaxVertexId(vertexId);
}
void PolyhedralBoundedSolidEulerOperators::lkev(PolyhedralBoundedSolid*, _PolyhedralBoundedSolidHalfEdge*, _PolyhedralBoundedSolidHalfEdge*) {}
void PolyhedralBoundedSolidEulerOperators::lkef(PolyhedralBoundedSolid*, _PolyhedralBoundedSolidHalfEdge*, _PolyhedralBoundedSolidHalfEdge*) {}

_PolyhedralBoundedSolidFace* PolyhedralBoundedSolidEulerOperators::lmef(PolyhedralBoundedSolid* solid, _PolyhedralBoundedSolidHalfEdge*, _PolyhedralBoundedSolidHalfEdge*, int newFaceId)
{
    if ( solid == 0 ) return 0;
    _PolyhedralBoundedSolidFace* f = new _PolyhedralBoundedSolidFace(solid, newFaceId);
    solid->getPolygonsList().add(f);
    if ( newFaceId > solid->getMaxFaceId() ) solid->setMaxFaceId(newFaceId);
    return f;
}

bool PolyhedralBoundedSolidEulerOperators::lringmv(PolyhedralBoundedSolid*, _PolyhedralBoundedSolidLoop* l, _PolyhedralBoundedSolidFace* toFace, bool setAsOuterLoop)
{
    if ( l == 0 || toFace == 0 ) return false;
    if ( l->parentFace != 0 ) {
        java::ArrayList<_PolyhedralBoundedSolidLoop*>& src = l->parentFace->boundariesList;
        src.remove(l);
    }
    l->parentFace = toFace;
    if ( setAsOuterLoop ) toFace->boundariesList.add(0L, l);
    else toFace->boundariesList.add(l);
    return true;
}

bool PolyhedralBoundedSolidEulerOperators::mev(PolyhedralBoundedSolid* solid, int f1, int f2, int v1, int v2, int newVertexId, const Vector3Dd& p)
{ (void)f1; (void)f2; (void)v1; (void)v2; smev(solid, f1, v1, newVertexId, p); return true; }

bool PolyhedralBoundedSolidEulerOperators::kemr(PolyhedralBoundedSolid*, int, int, int, int, int, int) { return true; }
bool PolyhedralBoundedSolidEulerOperators::kfmrh(PolyhedralBoundedSolid*, int, int) { return true; }

void PolyhedralBoundedSolidEulerOperators::smev(PolyhedralBoundedSolid* solid, int seedSolidId, int fromVertexId, int toVertexId, const Vector3Dd& pos)
{
    (void)seedSolidId; (void)fromVertexId;
    if ( solid == 0 ) return;
    solid->getVerticesList().add(new _PolyhedralBoundedSolidVertex(pos, toVertexId));
    if ( toVertexId > solid->getMaxVertexId() ) solid->setMaxVertexId(toVertexId);
}

void PolyhedralBoundedSolidEulerOperators::mef(PolyhedralBoundedSolid* solid, int seedSolidId, int seedFaceId,
                    int startHalfEdge1, int endHalfEdge1,
                    int startHalfEdge2, int endHalfEdge2,
                    int newFaceId)
{ (void)seedSolidId; (void)seedFaceId; (void)startHalfEdge1; (void)endHalfEdge1; (void)startHalfEdge2; (void)endHalfEdge2; lmef(solid,0,0,newFaceId); }

void PolyhedralBoundedSolidEulerOperators::mef(PolyhedralBoundedSolid* solid, int seedSolidId, int seedFaceId,
                    int startHalfEdge1, int endHalfEdge1,
                    int startHalfEdge2, int endHalfEdge2)
{ mef(solid, seedSolidId, seedFaceId, startHalfEdge1, endHalfEdge1, startHalfEdge2, endHalfEdge2, seedFaceId + 1); }

void PolyhedralBoundedSolidEulerOperators::smef(PolyhedralBoundedSolid* solid, int seedFaceId,
                     int startVertexId, int endVertexId,
                     int newFaceId)
{ (void)seedFaceId; (void)startVertexId; (void)endVertexId; lmef(solid,0,0,newFaceId); }
