#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
template <typename T>
long indexOf(const java::ArrayList<T>& list, T elem)
{
    for (long i = 0; i < list.size(); ++i) {
        if ( list.get(i) == elem ) {
            return i;
        }
    }
    return -1;
}

template <typename T>
void insertBefore(java::ArrayList<T>& list, T elem, T before)
{
    if ( list.size() == 0 ) {
        list.add(elem);
        return;
    }

    long pos = indexOf(list, before);
    if ( pos < 0 || pos >= list.size() ) {
        list.add(elem);
        return;
    }
    list.add(pos, elem);
}

template <typename T>
bool removeElem(java::ArrayList<T>& list, T elem)
{
    long pos = indexOf(list, elem);
    if ( pos < 0 ) return false;
    list.remove(pos);
    return true;
}

template <typename T>
void swapElems(java::ArrayList<T>& list, T a, T b)
{
    long ia = indexOf(list, a);
    long ib = indexOf(list, b);
    if ( ia < 0 || ib < 0 || ia == ib ) return;
    T tmp = list.get(ia);
    list.set(ia, list.get(ib));
    list.set(ib, tmp);
}

_PolyhedralBoundedSolidHalfEdge* addhe(
    PolyhedralBoundedSolid*,
    _PolyhedralBoundedSolidEdge* e,
    _PolyhedralBoundedSolidVertex* v,
    _PolyhedralBoundedSolidHalfEdge* where,
    int sign)
{
    if ( where == 0 || where->parentLoop == 0 || e == 0 ) {
        return 0;
    }

    _PolyhedralBoundedSolidHalfEdge* he = 0;
    if ( where->parentEdge == 0 ) {
        he = where;
    }
    else {
        he = new _PolyhedralBoundedSolidHalfEdge(v, where->parentLoop);
        insertBefore(where->parentLoop->halfEdgesList, he, where);
        he->startingVertex = v;
    }

    he->parentEdge = e;
    he->parentLoop = where->parentLoop;

    if ( sign == PolyhedralBoundedSolid::PLUS ) {
        e->leftHalf = he;
    }
    else {
        e->rightHalf = he;
    }

    return he;
}

bool failLringmv(PolyhedralBoundedSolid*, const char*)
{
    return false;
}

_PolyhedralBoundedSolidFace* validateLringmvInput(
    PolyhedralBoundedSolid*,
    _PolyhedralBoundedSolidLoop* loop,
    _PolyhedralBoundedSolidFace* destinationFace)
{
    if ( loop == 0 || destinationFace == 0 ) {
        return 0;
    }

    _PolyhedralBoundedSolidFace* sourceFace = loop->parentFace;
    if ( sourceFace == 0 ) {
        return 0;
    }
    if ( sourceFace->parentSolid != destinationFace->parentSolid ) {
        return 0;
    }
    return sourceFace;
}

void promoteLoopAsOuter(_PolyhedralBoundedSolidFace* face, _PolyhedralBoundedSolidLoop* loop)
{
    if ( face == 0 || loop == 0 ) return;
    if ( face->boundariesList.size() <= 0 || face->boundariesList.get(0) == loop ) {
        return;
    }
    swapElems(face->boundariesList, loop, face->boundariesList.get(0));
}

bool demoteLoopAsInner(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidFace* face,
    _PolyhedralBoundedSolidLoop* loop)
{
    if ( face == 0 || loop == 0 ) return failLringmv(solid, "");
    if ( face->boundariesList.size() <= 1 ) {
        return failLringmv(solid, "");
    }
    if ( face->boundariesList.get(0) != loop ) {
        return true;
    }
    swapElems(face->boundariesList, loop, face->boundariesList.get(1));
    return true;
}

bool reorderLoopInSameFace(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidLoop* loop,
    _PolyhedralBoundedSolidFace* face,
    bool setAsOuterLoop)
{
    if ( face == 0 || loop == 0 ) return failLringmv(solid, "");
    if ( indexOf(face->boundariesList, loop) < 0 ) {
        return failLringmv(solid, "");
    }
    if ( setAsOuterLoop ) {
        promoteLoopAsOuter(face, loop);
        return true;
    }
    return demoteLoopAsInner(solid, face, loop);
}

bool moveLoopAcrossFaces(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidLoop* loop,
    _PolyhedralBoundedSolidFace* sourceFace,
    _PolyhedralBoundedSolidFace* destinationFace,
    bool setAsOuterLoop)
{
    if ( sourceFace == 0 || destinationFace == 0 || loop == 0 ) {
        return failLringmv(solid, "");
    }
    if ( sourceFace->boundariesList.size() <= 1 ) {
        return failLringmv(solid, "");
    }
    if ( !setAsOuterLoop && destinationFace->boundariesList.size() <= 0 ) {
        return failLringmv(solid, "");
    }
    if ( !removeElem(sourceFace->boundariesList, loop) ) {
        return failLringmv(solid, "");
    }

    loop->parentFace = destinationFace;
    if ( setAsOuterLoop ) {
        destinationFace->boundariesList.add(0L, loop);
    }
    else {
        destinationFace->boundariesList.add(loop);
    }
    return true;
}

void insertLineDrawingEdge(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidHalfEdge* he,
    int vertexId,
    const Vector3Dd& p)
{
    if ( solid == 0 || he == 0 || he->startingVertex == 0 || he->parentLoop == 0 ) {
        return;
    }

    _PolyhedralBoundedSolidVertex* oldVertex = he->startingVertex;
    _PolyhedralBoundedSolidVertex* newVertex = new _PolyhedralBoundedSolidVertex(p, vertexId);
    _PolyhedralBoundedSolidEdge* newEdge = new _PolyhedralBoundedSolidEdge();

    solid->getVerticesList().add(newVertex);
    solid->getEdgesList().add(newEdge);

    addhe(solid, newEdge, oldVertex, he, PolyhedralBoundedSolid::PLUS);
    addhe(solid, newEdge, newVertex, he, PolyhedralBoundedSolid::MINUS);

    newVertex->emanatingHalfEdge = he->previous();
    oldVertex->emanatingHalfEdge = he;
}

void splitVertexNeighborhood(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidHalfEdge* he1,
    _PolyhedralBoundedSolidHalfEdge* he2,
    int vertexId,
    const Vector3Dd& p)
{
    if ( solid == 0 || he1 == 0 || he2 == 0 || he1->startingVertex == 0 ) {
        return;
    }

    _PolyhedralBoundedSolidEdge* newEdge = new _PolyhedralBoundedSolidEdge();
    _PolyhedralBoundedSolidVertex* newVertex = new _PolyhedralBoundedSolidVertex(p, vertexId);

    solid->getEdgesList().add(newEdge);
    solid->getVerticesList().add(newVertex);

    _PolyhedralBoundedSolidHalfEdge* he = he1;
    while ( he != 0 && he != he2 ) {
        he->startingVertex = newVertex;
        _PolyhedralBoundedSolidHalfEdge* mirror = he->mirrorHalfEdge();
        he = (mirror != 0) ? mirror->next() : 0;
    }

    addhe(solid, newEdge, newVertex, he2, PolyhedralBoundedSolid::PLUS);
    addhe(solid, newEdge, he2->startingVertex, he1, PolyhedralBoundedSolid::MINUS);

    newVertex->emanatingHalfEdge = he2->previous();
    he2->startingVertex->emanatingHalfEdge = he2;
}

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

void PolyhedralBoundedSolidEulerOperators::lmev(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidHalfEdge* he1,
    _PolyhedralBoundedSolidHalfEdge* he2,
    int vertexId,
    const Vector3Dd& p)
{
    if ( solid == 0 ) return;
    if ( he1 == 0 || he2 == 0 ) return;
    if ( he1->startingVertex != he2->startingVertex ) return;

    if ( vertexId > solid->getMaxVertexId() ) solid->setMaxVertexId(vertexId);

    if ( he1 == he2 ) {
        insertLineDrawingEdge(solid, he1, vertexId, p);
        return;
    }

    splitVertexNeighborhood(solid, he1, he2, vertexId, p);
}
void PolyhedralBoundedSolidEulerOperators::lkev(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidHalfEdge* he1,
    _PolyhedralBoundedSolidHalfEdge* he2)
{
    if ( solid == 0 || he1 == 0 || he2 == 0 ) return;
    if ( he1->parentEdge != he2->parentEdge ) return;
    if ( he1 == he2 ) return;

    _PolyhedralBoundedSolidHalfEdge* he = he2->next();
    while ( he != 0 && he != he1 ) {
        he->startingVertex = he2->startingVertex;
        _PolyhedralBoundedSolidHalfEdge* mirror = he->mirrorHalfEdge();
        he = (mirror != 0) ? mirror->next() : 0;
    }

    _PolyhedralBoundedSolidHalfEdge* he2next = he2->next();
    he1->parentLoop->unlistHalfEdge(he1);
    he2->parentLoop->unlistHalfEdge(he2);
    he2->startingVertex->emanatingHalfEdge = he2next;
    if ( he2->parentLoop->halfEdgesList.size() < 1 ) {
        he2->startingVertex->emanatingHalfEdge = 0;
    }

    removeElem(solid->getEdgesList(), he1->parentEdge);
    removeElem(solid->getVerticesList(), he1->startingVertex);

    if ( he2->parentLoop->halfEdgesList.size() <= 0 ) {
        he2->parentEdge = 0;
        he2->parentLoop->halfEdgesList.add(he2);
        he2->parentLoop->boundaryStartHalfEdge = he2;
    }
}

void PolyhedralBoundedSolidEulerOperators::lkef(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidHalfEdge* he1,
    _PolyhedralBoundedSolidHalfEdge* he2)
{
    if ( solid == 0 || he1 == 0 || he2 == 0 ) return;
    if ( he1->parentEdge != he2->parentEdge ) return;
    if ( he1->parentLoop == 0 || he2->parentLoop == 0 ) return;
    if ( he1->parentLoop->parentFace == he2->parentLoop->parentFace ) return;

    _PolyhedralBoundedSolidHalfEdge* halfEdgePivot = he1->next();
    _PolyhedralBoundedSolidEdge* edgeToBeKilled = he1->parentEdge;
    _PolyhedralBoundedSolidLoop* loopToBeKilled = he2->parentLoop;
    _PolyhedralBoundedSolidFace* faceToBeKilled = loopToBeKilled->parentFace;

    java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> migratedHalfEdges;
    _PolyhedralBoundedSolidHalfEdge* he = he2->next();
    int maxTraversal = (int)he2->parentLoop->halfEdgesList.size() + 1;
    int traversed = 0;
    while ( he != he2 ) {
        if ( he == 0 || traversed > maxTraversal ) {
            migratedHalfEdges.clear();
            for (long idx = 0; idx < he2->parentLoop->halfEdgesList.size(); ++idx) {
                _PolyhedralBoundedSolidHalfEdge* candidate =
                    he2->parentLoop->halfEdgesList.get(idx);
                if ( candidate != he2 ) {
                    migratedHalfEdges.add(candidate);
                }
            }
            break;
        }
        migratedHalfEdges.add(he);
        he = he->next();
        traversed++;
    }

    he1->parentLoop->unlistHalfEdge(he1);
    he2->parentLoop->unlistHalfEdge(he2);
    removeElem(solid->getEdgesList(), edgeToBeKilled);
    removeElem(faceToBeKilled->boundariesList, loopToBeKilled);
    removeElem(solid->getPolygonsList(), faceToBeKilled);

    for (long i = migratedHalfEdges.size() - 1; i >= 0; --i) {
        he = migratedHalfEdges.get(i);
        he->parentLoop = he1->parentLoop;
        insertBefore(he1->parentLoop->halfEdgesList, he, halfEdgePivot);
        halfEdgePivot = he;
    }

    while ( faceToBeKilled->boundariesList.size() > 0 ) {
        _PolyhedralBoundedSolidLoop* orphanedLoop = faceToBeKilled->boundariesList.get(0);
        orphanedLoop->parentFace = he1->parentLoop->parentFace;
        faceToBeKilled->boundariesList.remove(0L);
        he1->parentLoop->parentFace->boundariesList.add(orphanedLoop);
    }
}

_PolyhedralBoundedSolidFace* PolyhedralBoundedSolidEulerOperators::lmef(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidHalfEdge* he1,
    _PolyhedralBoundedSolidHalfEdge* he2,
    int newFaceId)
{
    if ( solid == 0 || he1 == 0 || he2 == 0 ) return 0;
    if ( he1->parentLoop == 0 || he2->parentLoop == 0 ) return 0;
    if ( he1->startingVertex == 0 || he2->startingVertex == 0 ) return 0;

    if ( newFaceId > solid->getMaxFaceId() ) solid->setMaxFaceId(newFaceId);

    _PolyhedralBoundedSolidFace* newFace = new _PolyhedralBoundedSolidFace(solid, newFaceId);
    _PolyhedralBoundedSolidLoop* oldLoop = he1->parentLoop;
    _PolyhedralBoundedSolidLoop* newLoop = new _PolyhedralBoundedSolidLoop(newFace);
    _PolyhedralBoundedSolidEdge* newEdge = new _PolyhedralBoundedSolidEdge();

    solid->getPolygonsList().add(newFace);
    solid->getEdgesList().add(newEdge);

    java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> migratedHalfEdges;
    _PolyhedralBoundedSolidHalfEdge* he = he1;
    while ( he != 0 && he != he2 ) {
        migratedHalfEdges.add(he);
        he = he->next();
        if ( he == he1 ) break;
    }

    for (long i = 0; i < migratedHalfEdges.size(); ++i) {
        he = migratedHalfEdges.get(i);
        he->parentLoop = newLoop;
        oldLoop->unlistHalfEdge(he);
        newLoop->halfEdgesList.add(he);
    }

    _PolyhedralBoundedSolidHalfEdge* nhe1 =
        addhe(solid, newEdge, he2->startingVertex, he1, PolyhedralBoundedSolid::MINUS);
    _PolyhedralBoundedSolidHalfEdge* nhe2 =
        addhe(solid, newEdge, he1->startingVertex, he2, PolyhedralBoundedSolid::PLUS);

    if ( nhe1 == 0 || nhe2 == 0 ) {
        return 0;
    }

    newLoop->boundaryStartHalfEdge = nhe1;
    he2->parentLoop->boundaryStartHalfEdge = nhe2;

    return newFace;
}

void PolyhedralBoundedSolidEulerOperators::lkemr(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidHalfEdge* he1,
    _PolyhedralBoundedSolidHalfEdge* he2)
{
    if ( solid == 0 || he1 == 0 || he2 == 0 ) return;

    _PolyhedralBoundedSolidLoop* oldLoop = he1->parentLoop;
    if ( oldLoop == 0 ) return;
    _PolyhedralBoundedSolidLoop* newLoop = new _PolyhedralBoundedSolidLoop(oldLoop->parentFace);
    _PolyhedralBoundedSolidEdge* killedEdge = he1->parentEdge;

    java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> migratedHalfEdges;
    _PolyhedralBoundedSolidHalfEdge* he4 = he1->next();
    if ( he4 == 0 ) return;
    do {
        migratedHalfEdges.add(he4);
        if ( he4 == he2 ) break;
        he4 = he4->next();
    } while ( he4 != 0 && he4 != he2 );

    for (long i = 0; i < migratedHalfEdges.size(); ++i) {
        _PolyhedralBoundedSolidHalfEdge* he3 = migratedHalfEdges.get(i);
        removeElem(oldLoop->halfEdgesList, he3);
        newLoop->halfEdgesList.add(he3);
        he3->parentLoop = newLoop;
    }
    if ( newLoop->halfEdgesList.size() > 0 ) {
        newLoop->boundaryStartHalfEdge = newLoop->halfEdgesList.get(0);
    }

    oldLoop->delhe(he1);
    oldLoop->delhe(he2);

    if ( newLoop->halfEdgesList.size() <= 1 ) {
        if ( newLoop->boundaryStartHalfEdge != 0 ) {
            newLoop->boundaryStartHalfEdge->parentEdge = 0;
            newLoop->halfEdgesList.get(0)->startingVertex->emanatingHalfEdge = 0;
        }
    }

    if ( oldLoop->halfEdgesList.size() <= 1 ) {
        if ( oldLoop->boundaryStartHalfEdge != 0 ) {
            oldLoop->boundaryStartHalfEdge->parentEdge = 0;
            oldLoop->halfEdgesList.get(0)->startingVertex->emanatingHalfEdge = 0;
        }
    }

    removeElem(solid->getEdgesList(), killedEdge);
}

void PolyhedralBoundedSolidEulerOperators::lkfmrh(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidFace* face1,
    _PolyhedralBoundedSolidFace* face2)
{
    if ( solid == 0 || face1 == 0 || face2 == 0 ) return;
    if ( face2->boundariesList.size() > 1 ) return;
    if ( face2->boundariesList.size() <= 0 ) return;

    _PolyhedralBoundedSolidLoop* oldLoop = face2->boundariesList.get(0);
    _PolyhedralBoundedSolidLoop* newLoop = new _PolyhedralBoundedSolidLoop(face1);

    for (long i = 0; i < oldLoop->halfEdgesList.size(); ++i) {
        _PolyhedralBoundedSolidHalfEdge* he = oldLoop->halfEdgesList.get(i);
        he->parentLoop = newLoop;
        newLoop->halfEdgesList.add(he);
    }
    if ( newLoop->halfEdgesList.size() > 0 ) {
        newLoop->boundaryStartHalfEdge = newLoop->halfEdgesList.get(0);
    }

    removeElem(solid->getPolygonsList(), face2);
}

_PolyhedralBoundedSolidFace* PolyhedralBoundedSolidEulerOperators::lmfkrh(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidLoop* l,
    int newFaceId)
{
    if ( solid == 0 || l == 0 || l->parentFace == 0 ) return 0;
    _PolyhedralBoundedSolidFace* newFace = new _PolyhedralBoundedSolidFace(solid, newFaceId);
    solid->getPolygonsList().add(newFace);

    if ( newFaceId > solid->getMaxFaceId() ) solid->setMaxFaceId(newFaceId);

    removeElem(l->parentFace->boundariesList, l);
    l->parentFace = newFace;
    newFace->boundariesList.add(l);

    return newFace;
}

void PolyhedralBoundedSolidEulerOperators::lkimrh(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidFace* face1,
    _PolyhedralBoundedSolidFace* face2)
{
    lkfmrh(solid, face1, face2);
}

_PolyhedralBoundedSolidFace* PolyhedralBoundedSolidEulerOperators::lmikrh(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidLoop* l,
    int newFaceId)
{
    return lmfkrh(solid, l, newFaceId);
}

bool PolyhedralBoundedSolidEulerOperators::lringmv(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidLoop* l,
    _PolyhedralBoundedSolidFace* toFace,
    bool setAsOuterLoop)
{
    _PolyhedralBoundedSolidFace* fromFace = validateLringmvInput(solid, l, toFace);
    if ( fromFace == 0 ) {
        return false;
    }
    if ( fromFace == toFace ) {
        return reorderLoopInSameFace(solid, l, toFace, setAsOuterLoop);
    }
    return moveLoopAcrossFaces(solid, l, fromFace, toFace, setAsOuterLoop);
}

void PolyhedralBoundedSolidEulerOperators::lmekr(
    PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidHalfEdge* he1,
    _PolyhedralBoundedSolidHalfEdge* he2)
{
    if ( solid == 0 || he1 == 0 || he2 == 0 ) return;
    if ( he1->parentLoop == he2->parentLoop ) return;
    if ( he1->parentLoop == 0 || he2->parentLoop == 0 ) return;
    if ( he1->parentLoop->parentFace != he2->parentLoop->parentFace ) return;

    java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> migratedHalfEdges;
    _PolyhedralBoundedSolidLoop* ringToKill = he2->parentLoop;
    _PolyhedralBoundedSolidHalfEdge* he = he2;
    do {
        migratedHalfEdges.add(he);
        he = he->next();
    } while ( he != 0 && he != he2 );

    while ( ringToKill->halfEdgesList.size() > 0 ) {
        ringToKill->halfEdgesList.remove(0L);
    }
    removeElem(ringToKill->parentFace->boundariesList, ringToKill);

    _PolyhedralBoundedSolidVertex* v1 = he1->startingVertex;
    _PolyhedralBoundedSolidVertex* v2 = he2->startingVertex;
    _PolyhedralBoundedSolidEdge* newEdge = new _PolyhedralBoundedSolidEdge();
    solid->getEdgesList().add(newEdge);

    _PolyhedralBoundedSolidHalfEdge* heLast =
        addhe(solid, newEdge, v2, he1, PolyhedralBoundedSolid::MINUS);
    newEdge->rightHalf = addhe(solid, newEdge, v1, heLast, PolyhedralBoundedSolid::MINUS);
    newEdge->leftHalf = heLast;

    for (long i = 0; i < migratedHalfEdges.size() && migratedHalfEdges.size() > 1; ++i) {
        he = migratedHalfEdges.get(i);
        he->parentLoop = he1->parentLoop;
        insertBefore(he1->parentLoop->halfEdgesList, he, heLast);
    }
}

bool PolyhedralBoundedSolidEulerOperators::mev(
    PolyhedralBoundedSolid* solid,
    int f1,
    int f2,
    int v1,
    int v2,
    int v3,
    int newVertexId,
    const Vector3Dd& p)
{
    if ( solid == 0 ) return false;

    _PolyhedralBoundedSolidFace* oldFace1 = solid->findFace(f1);
    if ( oldFace1 == 0 ) return false;
    _PolyhedralBoundedSolidFace* oldFace2 = solid->findFace(f2);
    if ( oldFace2 == 0 ) return false;

    _PolyhedralBoundedSolidHalfEdge* he1 = oldFace1->findHalfEdge(v1, v2);
    if ( he1 == 0 ) return false;
    _PolyhedralBoundedSolidHalfEdge* he2 = oldFace2->findHalfEdge(v1, v3);
    if ( he2 == 0 ) return false;

    lmev(solid, he1, he2, newVertexId, p);
    return true;
}

bool PolyhedralBoundedSolidEulerOperators::mev(
    PolyhedralBoundedSolid* solid,
    int f1,
    int f2,
    int v1,
    int v2,
    int newVertexId,
    const Vector3Dd& p)
{
    return mev(solid, f1, f2, v1, v2, v2, newVertexId, p);
}

bool PolyhedralBoundedSolidEulerOperators::kemr(
    PolyhedralBoundedSolid* solid,
    int f1,
    int f2,
    int v1,
    int v2,
    int v3,
    int v4)
{
    if ( solid == 0 ) return false;

    _PolyhedralBoundedSolidFace* oldFace1 = solid->findFace(f1);
    if ( oldFace1 == 0 ) return false;
    _PolyhedralBoundedSolidFace* oldFace2 = solid->findFace(f2);
    if ( oldFace2 == 0 ) return false;

    _PolyhedralBoundedSolidHalfEdge* he1 = oldFace1->findHalfEdge(v1, v2);
    if ( he1 == 0 ) return false;
    _PolyhedralBoundedSolidHalfEdge* he2 = oldFace2->findHalfEdge(v3, v4);
    if ( he2 == 0 ) return false;

    lkemr(solid, he1, he2);
    return true;
}

bool PolyhedralBoundedSolidEulerOperators::kfmrh(PolyhedralBoundedSolid* solid, int f1, int f2)
{
    if ( solid == 0 ) return false;
    _PolyhedralBoundedSolidFace* oldFace1 = solid->findFace(f1);
    if ( oldFace1 == 0 ) return false;
    _PolyhedralBoundedSolidFace* oldFace2 = solid->findFace(f2);
    if ( oldFace2 == 0 ) return false;
    lkfmrh(solid, oldFace1, oldFace2);
    return true;
}

bool PolyhedralBoundedSolidEulerOperators::smev(
    PolyhedralBoundedSolid* solid,
    int seedSolidId,
    int fromVertexId,
    int toVertexId,
    const Vector3Dd& pos)
{
    if ( solid == 0 ) return false;

    _PolyhedralBoundedSolidFace* oldFace = solid->findFace(seedSolidId);
    if ( oldFace == 0 ) return false;

    _PolyhedralBoundedSolidHalfEdge* he1 = oldFace->findHalfEdge(fromVertexId);
    if ( he1 == 0 ) return false;

    lmev(solid, he1, he1, toVertexId, pos);
    return true;
}

bool PolyhedralBoundedSolidEulerOperators::mef(
    PolyhedralBoundedSolid* solid,
    int seedSolidId,
    int seedFaceId,
    int startHalfEdge1,
    int endHalfEdge1,
    int startHalfEdge2,
    int endHalfEdge2,
    int newFaceId)
{
    (void)seedSolidId;
    if ( solid == 0 ) return false;

    _PolyhedralBoundedSolidFace* oldFace = solid->findFace(seedFaceId);
    if ( oldFace == 0 ) return false;

    _PolyhedralBoundedSolidHalfEdge* he1 = oldFace->findHalfEdge(startHalfEdge1, endHalfEdge1);
    _PolyhedralBoundedSolidHalfEdge* he2 = oldFace->findHalfEdge(startHalfEdge2, endHalfEdge2);
    if ( he1 == 0 || he2 == 0 ) return false;

    lmef(solid, he1, he2, newFaceId);
    return true;
}

bool PolyhedralBoundedSolidEulerOperators::mef(
    PolyhedralBoundedSolid* solid,
    int seedSolidId,
    int seedFaceId,
    int startHalfEdge1,
    int endHalfEdge1,
    int startHalfEdge2,
    int endHalfEdge2)
{
    return mef(solid, seedSolidId, seedFaceId,
        startHalfEdge1, endHalfEdge1, startHalfEdge2, endHalfEdge2,
        seedFaceId + 1);
}

bool PolyhedralBoundedSolidEulerOperators::smef(
    PolyhedralBoundedSolid* solid,
    int seedFaceId,
    int startVertexId,
    int endVertexId,
    int newFaceId)
{
    if ( solid == 0 ) return false;

    _PolyhedralBoundedSolidFace* oldFace = solid->findFace(seedFaceId);
    if ( oldFace == 0 ) return false;

    _PolyhedralBoundedSolidHalfEdge* he1 = oldFace->findHalfEdge(startVertexId);
    _PolyhedralBoundedSolidHalfEdge* he2 = oldFace->findHalfEdge(endVertexId);
    if ( he1 == 0 || he2 == 0 ) return false;

    lmef(solid, he1, he2, newFaceId);
    return true;
}
