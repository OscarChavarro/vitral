#ifndef __POLYHEDRAL_BOUNDED_SOLID_LOOP__
#define __POLYHEDRAL_BOUNDED_SOLID_LOOP__

#include "java/util/ArrayList.h"
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;

class _PolyhedralBoundedSolidLoop {
public:
    _PolyhedralBoundedSolidFace* parentFace;
    _PolyhedralBoundedSolidHalfEdge* boundaryStartHalfEdge;
    java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> halfEdgesList;

    explicit _PolyhedralBoundedSolidLoop(_PolyhedralBoundedSolidFace* parent);
    ~_PolyhedralBoundedSolidLoop();

    void unlistHalfEdge(_PolyhedralBoundedSolidHalfEdge* he);
    _PolyhedralBoundedSolidHalfEdge* halfEdgeVertices(int a, int b);
    _PolyhedralBoundedSolidHalfEdge* firstHalfEdgeAtVertex(int a);
    void delhe(_PolyhedralBoundedSolidHalfEdge* he);
    void revert();

    _PolyhedralBoundedSolidHalfEdge* previousOf(_PolyhedralBoundedSolidHalfEdge* he) const;
    _PolyhedralBoundedSolidHalfEdge* nextOf(_PolyhedralBoundedSolidHalfEdge* he) const;
};

#endif
