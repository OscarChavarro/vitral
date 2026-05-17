#ifndef __VSDK_PBS_NODE_FACE_H__
#define __VSDK_PBS_NODE_FACE_H__

#include <vector>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class PolyhedralBoundedSolid;
class InfinitePlane;
class _PolyhedralBoundedSolidLoop;
class _PolyhedralBoundedSolidHalfEdge;

class _PolyhedralBoundedSolidFace {
public:
    int id;
    PolyhedralBoundedSolid* parentSolid;
    std::vector<_PolyhedralBoundedSolidLoop*> boundariesList;

    _PolyhedralBoundedSolidFace(PolyhedralBoundedSolid* parent, int id);

    _PolyhedralBoundedSolidHalfEdge* findHalfEdge(int vn1, int vn2);
    _PolyhedralBoundedSolidHalfEdge* findHalfEdge(int vn1);
    bool calculatePlane();
    InfinitePlane* getContainingPlane();
    int testPointInside(const Vector3Dd& point, double tolerance);
    void revert();
};

#endif
