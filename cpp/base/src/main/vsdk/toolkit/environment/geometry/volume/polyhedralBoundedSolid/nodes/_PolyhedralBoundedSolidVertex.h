#ifndef __VSDK_PBS_NODE_VERTEX_H__
#define __VSDK_PBS_NODE_VERTEX_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"

class _PolyhedralBoundedSolidHalfEdge;

class _PolyhedralBoundedSolidVertex {
public:
    int id;
    Vector3Dd position;
    _PolyhedralBoundedSolidHalfEdge* emanatingHalfEdge;
    ColorRgb debugColor;

    _PolyhedralBoundedSolidVertex(const Vector3Dd& position, int id)
        : id(id), position(position), emanatingHalfEdge(0), debugColor(1, 0, 0) {}
};

#endif
