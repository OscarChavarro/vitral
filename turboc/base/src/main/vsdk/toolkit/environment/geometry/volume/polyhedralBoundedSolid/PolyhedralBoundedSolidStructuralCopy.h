//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef __PBS_STRUCTURAL_COPY__
#define __PBS_STRUCTURAL_COPY__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/TreeMap.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidVertex;
class _PolyhedralBoundedSolidEdge;

/**
Structural copy of a half-edge solid, in the spirit of the copy routine
from [MANT1988] Sec. 15.8 ("scopy"): every vertex, edge, face, loop and
half-edge is duplicated and all the cross references are remapped to the
duplicates, keeping identifiers, list order and loop start half-edges.

Unlike a serialization round trip it allocates no byte buffers and uses no
recursion, so its cost is linear in the size of the boundary and it cannot
overflow the stack on large models.

C++ port note: Java identity hash maps are `TreeMap`s keyed by node
address; the caller owns (and deletes) the returned solid.
*/
class PolyhedralBoundedSolidStructuralCopy {
private:
    PolyhedralBoundedSolidStructuralCopy();

    static _PolyhedralBoundedSolidVertex* mapVertex(
        TreeMap<_PolyhedralBoundedSolidVertex*,
            _PolyhedralBoundedSolidVertex*>& vertices,
        PolyhedralBoundedSolid* target,
        _PolyhedralBoundedSolidVertex* v);
    static _PolyhedralBoundedSolidEdge* mapEdge(
        TreeMap<_PolyhedralBoundedSolidEdge*,
            _PolyhedralBoundedSolidEdge*>& edges,
        PolyhedralBoundedSolid* target,
        _PolyhedralBoundedSolidEdge* e);

public:
    /**
    Returns an independent copy of `source`; NULL for NULL. The copy
    shares no node and no position vector with the source. Half-edges
    without a parent edge (loops of a single vertex, as left by mvfs or
    lkemr) are copied as they are.
    */
    static PolyhedralBoundedSolid* copy(PolyhedralBoundedSolid* source);
};

#endif
