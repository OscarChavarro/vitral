#include "PolyhedralBoundedSolidEulerOperators.h"

#include "PolyhedralBoundedSolid.h"
#include "nodes/_PolyhedralBoundedSolidFace.h"
#include "nodes/_PolyhedralBoundedSolidLoop.h"
#include "nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "nodes/_PolyhedralBoundedSolidEdge.h"
#include "nodes/_PolyhedralBoundedSolidVertex.h"

#include <algorithm>

void PolyhedralBoundedSolidEulerOperators::mvfs(PolyhedralBoundedSolid* solid, const Vector3Dd& pos, int vertexId, int faceId)
{
    if ( solid == 0 ) return;
    _PolyhedralBoundedSolidFace* newFace = new _PolyhedralBoundedSolidFace(solid, faceId);
    _PolyhedralBoundedSolidLoop* newLoop = new _PolyhedralBoundedSolidLoop(newFace);
    _PolyhedralBoundedSolidVertex* newVertex = new _PolyhedralBoundedSolidVertex(pos, vertexId);
    _PolyhedralBoundedSolidHalfEdge* newHalfEdge = new _PolyhedralBoundedSolidHalfEdge(newVertex, newLoop);
    newLoop->halfEdgesList.push_back(newHalfEdge);
    newLoop->boundaryStartHalfEdge = newHalfEdge;

    solid->getPolygonsList().push_back(newFace);
    solid->getVerticesList().push_back(newVertex);
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
    solid->getVerticesList().push_back(new _PolyhedralBoundedSolidVertex(p, vertexId));
    if ( vertexId > solid->getMaxVertexId() ) solid->setMaxVertexId(vertexId);
}
void PolyhedralBoundedSolidEulerOperators::lkev(PolyhedralBoundedSolid*, _PolyhedralBoundedSolidHalfEdge*, _PolyhedralBoundedSolidHalfEdge*) {}
void PolyhedralBoundedSolidEulerOperators::lkef(PolyhedralBoundedSolid*, _PolyhedralBoundedSolidHalfEdge*, _PolyhedralBoundedSolidHalfEdge*) {}

_PolyhedralBoundedSolidFace* PolyhedralBoundedSolidEulerOperators::lmef(PolyhedralBoundedSolid* solid, _PolyhedralBoundedSolidHalfEdge*, _PolyhedralBoundedSolidHalfEdge*, int newFaceId)
{
    if ( solid == 0 ) return 0;
    _PolyhedralBoundedSolidFace* f = new _PolyhedralBoundedSolidFace(solid, newFaceId);
    solid->getPolygonsList().push_back(f);
    if ( newFaceId > solid->getMaxFaceId() ) solid->setMaxFaceId(newFaceId);
    return f;
}

bool PolyhedralBoundedSolidEulerOperators::lringmv(PolyhedralBoundedSolid*, _PolyhedralBoundedSolidLoop* l, _PolyhedralBoundedSolidFace* toFace, bool setAsOuterLoop)
{
    if ( l == 0 || toFace == 0 ) return false;
    if ( l->parentFace != 0 ) {
        std::vector<_PolyhedralBoundedSolidLoop*>& src = l->parentFace->boundariesList;
        src.erase(std::remove(src.begin(), src.end(), l), src.end());
    }
    l->parentFace = toFace;
    if ( setAsOuterLoop ) toFace->boundariesList.insert(toFace->boundariesList.begin(), l);
    else toFace->boundariesList.push_back(l);
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
    solid->getVerticesList().push_back(new _PolyhedralBoundedSolidVertex(pos, toVertexId));
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
