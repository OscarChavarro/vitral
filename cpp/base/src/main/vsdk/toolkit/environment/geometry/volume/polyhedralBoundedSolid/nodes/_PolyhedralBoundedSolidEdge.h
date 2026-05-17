#ifndef __VSDK_PBS_NODE_EDGE_H__
#define __VSDK_PBS_NODE_EDGE_H__

#include "vsdk/toolkit/common/color/ColorRgb.h"

class _PolyhedralBoundedSolidHalfEdge;

class _PolyhedralBoundedSolidEdge {
public:
    _PolyhedralBoundedSolidHalfEdge* rightHalf;
    _PolyhedralBoundedSolidHalfEdge* leftHalf;
    int id;
    ColorRgb debugColor;

    _PolyhedralBoundedSolidEdge();
    int getEndingVertexId() const;
    int getStartingVertexId() const;
};

#endif
