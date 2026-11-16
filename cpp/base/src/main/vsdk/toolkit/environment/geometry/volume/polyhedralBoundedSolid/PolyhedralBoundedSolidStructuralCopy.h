//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef __POLYHEDRAL_BOUNDED_SOLID_STRUCTURAL_COPY__
#define __POLYHEDRAL_BOUNDED_SOLID_STRUCTURAL_COPY__

#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidEdge;
class _PolyhedralBoundedSolidVertex;

/**
Structural copy of a half-edge solid, in the spirit of the copy routine
from [MANT1988] Sec. 15.8 ("scopy"): every vertex, edge, face, loop and
half-edge is duplicated and all the cross references are remapped to the
duplicates, keeping identifiers, list order and loop start half-edges.

Unlike a serialization round trip it allocates no byte buffers and uses no
recursion, so its cost is linear in the size of the boundary and it cannot
overflow the stack on large models.

C++ port note: the Java `IdentityHashMap`s are `java::HashMap`s keyed by
pointer; the keys are also kept in lists, in insertion order, to visit
the entries (the Java code visits them in no particular order, and the
visit order does not change the copy).
*/
class PolyhedralBoundedSolidStructuralCopy {
public:
    /**
    Returns an independent copy of `source`; null for null. The copy
    shares no node and no position vector with the source. Half-edges
    without a parent edge (loops of a single vertex, as left by mvfs or
    lkemr) are copied as they are.
    @return a new solid, owned by the caller
    */
    static PolyhedralBoundedSolid* copy(PolyhedralBoundedSolid* source);

private:
    PolyhedralBoundedSolidStructuralCopy();

    /** A vertex referenced by a loop but missing from the vertex list. */
    static _PolyhedralBoundedSolidVertex* mapVertex(
        java::HashMap<_PolyhedralBoundedSolidVertex*,
            _PolyhedralBoundedSolidVertex*>& vertices,
        java::ArrayList<_PolyhedralBoundedSolidVertex*>& vertexKeys,
        PolyhedralBoundedSolid* target,
        _PolyhedralBoundedSolidVertex* v);

    /** An edge referenced by a half-edge but missing from the edge list. */
    static _PolyhedralBoundedSolidEdge* mapEdge(
        java::HashMap<_PolyhedralBoundedSolidEdge*,
            _PolyhedralBoundedSolidEdge*>& edges,
        java::ArrayList<_PolyhedralBoundedSolidEdge*>& edgeKeys,
        PolyhedralBoundedSolid* target,
        _PolyhedralBoundedSolidEdge* e);
};

#endif
