//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_VERTEX_NEIGHBORHOOD__
#define ___PBS_VERTEX_NEIGHBORHOOD__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSectorClassification.h"

class InfinitePlane;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidVertex;

/**
Neighbourhood of a vertex classified against a plane through it, following
section [MANT1988].14.5.2 and program [MANT1988].14.4. Problem
[MANT1988].15.4 observes that the vertex/face classifier of the set
operations is the same procedure as the splitter's; both build their
neighbourhood here (Fable5.2Analysis.md pending item 11) and keep their own
reclassification rules: [MANT1988].14.5 for the splitter, table
[MANT1988].15.3 (which depends on the operation) for the set operations.
*/
class _PolyhedralBoundedSolidVertexNeighborhood
    : public _PolyhedralBoundedSolidOperator {
private:
    _PolyhedralBoundedSolidVertexNeighborhood();

    static Vector3Dd unit(const Vector3Dd& v);
    static int compareToZero(double value);

public:
    /**
    Classifies the neighbourhood of `vtx` against `plane`. The loop turns
    with mirror(he).next(), so the sector of `he` (its face, between
    he.previous() and he) lies between the entry of the previous half-edge
    and the entry of `he`: an entry for the far end of each edge, preceded
    by an entry for the bisector of the sector when the sector is reflex.
    Convex and flat sectors are bounded by their two edge entries; a
    reflex one is split by its bisector into two sectors narrower than a
    half-turn, as the book requires. Edge entries that lie on the plane are
    tagged INPLANE_EDGE.

    C++ port note: Java is generic on the entry type (a factory makes
    them); here the entries are of the base type, appended to `neighbors`,
    and each caller converts them to its own entry type.
    */
    static void classify(_PolyhedralBoundedSolidVertex* vtx,
        const InfinitePlane& plane,
        ArrayList<_PolyhedralBoundedSolidSectorClassification>& neighbors);

    /**
    True when the face sector at he.startingVertex, between he.previous()
    and he, is reflex: (next - v) x (prev - v) points against the face
    normal (loops run counterclockwise about the normal). A flat sector (a
    vertex inserted inside an edge) is not: its two edge entries already
    bound it, and a bisector sample there lies on the plane whenever the
    face crosses it, which the edge reclassification would then turn into a
    spurious entry. The test compares the sine of the angle, so it does not
    depend on the size of the model.
    */
    static BOOLEAN isReflexSector(_PolyhedralBoundedSolidHalfEdge* he);

    /**
    A point on the bisector of the reflex face corner at
    `he.startingVertex`, inside the face, at half the length of the shorter
    of its two edges: only its side of a plane through the vertex is used,
    and at that distance the comparison with the tolerance is relative to
    the local size of the model. For a reflex corner the sum of the two
    unit edge directions points out of the face, so it is negated.
    */
    static Vector3Dd inwardReflexBisector(_PolyhedralBoundedSolidHalfEdge* he);
};

#endif
