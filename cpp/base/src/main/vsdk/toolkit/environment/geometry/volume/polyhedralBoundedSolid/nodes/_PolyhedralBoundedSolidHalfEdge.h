#ifndef __VSDK_PBS_NODE_HALFEDGE_H__
#define __VSDK_PBS_NODE_HALFEDGE_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class _PolyhedralBoundedSolidLoop;
class _PolyhedralBoundedSolidEdge;
class _PolyhedralBoundedSolidVertex;

class _PolyhedralBoundedSolidHalfEdge {
public:
    static const int LEFT_SIDE = 1;
    static const int RIGHT_SIDE = 2;
    static const int NO_SIDE = 3;

    _PolyhedralBoundedSolidLoop* parentLoop;
    _PolyhedralBoundedSolidEdge* parentEdge;
    _PolyhedralBoundedSolidVertex* startingVertex;
    int id;

    _PolyhedralBoundedSolidHalfEdge(_PolyhedralBoundedSolidVertex* v, _PolyhedralBoundedSolidLoop* parentLoop);

    _PolyhedralBoundedSolidHalfEdge* previous();
    _PolyhedralBoundedSolidHalfEdge* next();
    _PolyhedralBoundedSolidHalfEdge* mirrorHalfEdge();
    bool vertexPositionMatch(_PolyhedralBoundedSolidHalfEdge* other, double tolerance);
};

#endif
