//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___POLYHEDRAL_BOUNDED_SOLID_SET_NON_INTERSECTING_CLASSIFIER__
#define ___POLYHEDRAL_BOUNDED_SOLID_SET_NON_INTERSECTING_CLASSIFIER__

#include "java/util/Collections.h"
#include "java/util/ArrayList.h"

#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"

class InfinitePlane;
class PolyhedralBoundedSolidFaceGeometry;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidLoop;
class _PolyhedralBoundedSolidVertex;

/**
Encapsulates preflight classification and no-intersection resolution for
Boolean set operations. This keeps containment/touching policy separate from
the main intersection/splitting pipeline.

C++ counterpart of Java's `_PolyhedralBoundedSolidSetNonIntersectingClassifier`.
Java methods returning a null solid return a null pointer here; the result
solid is `outRes` itself, owned by the caller.
*/
class _PolyhedralBoundedSolidSetNonIntersectingClassifier
    : public _PolyhedralBoundedSolidOperator {
public:
    /**
    Per-setOp memo for the expensive, order-independent preflight
    predicates. Within a single Boolean operation the operand solids are not
    mutated until the Mantyla generate stage runs, so each predicate is a pure
    function of `(a, b)` and may be computed once and reused across
    runTouchingOnlyPreflightCase,
    runContainmentOnlyPreflightCase and runSetOpNoIntersectionCase. The cache
    does not change any decision.
    */
    class _PreflightCache {
    public:
        _PreflightCache(PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b);

        int aInB();
        int bInA();
        bool hasInteriorOverlap();
        bool hasEdgeFaceIntersectionAB();
        bool hasEdgeFaceIntersectionBA();

    private:
        static const int UNSET;

        PolyhedralBoundedSolid* a;
        PolyhedralBoundedSolid* b;
        int aInBValue;
        int bInAValue;
        signed char interiorOverlap;
        signed char edgeFaceAB;
        signed char edgeFaceBA;
    };

    /**
    Builds a fresh per-setOp preflight memo for the operand pair. The caller
    is responsible for using it only while `inSolidA`/`inSolidB` remain
    unmutated (i.e. across the preflight block, before the generate stage).
    @param inSolidA first operand
    @param inSolidB second operand
    @return new memo
    */
    static _PreflightCache newPreflightCache(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB);

    /**
    Section 7.3.1.D preflight: detects the case where one solid is strictly
    contained in the other (A in B or B in A) without any real edge/face
    intersection, where the regular pipeline would produce an empty result
    although the result must be one of the operands per [MANT1988].15.1.
    @param inSolidA first operand
    @param inSolidB second operand
    @return true when the no-intersection dispatch applies
    */
    static bool runContainmentOnlyPreflightCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB);
    static bool runContainmentOnlyPreflightCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        _PreflightCache& cache);

    /**
    Detects solids that only touch each other (no interior overlap, no proper
    edge/face crossing and no partial coplanar face overlap).
    @param inSolidA first operand
    @param inSolidB second operand
    @return true when the no-intersection dispatch applies
    */
    static bool runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB);
    static bool runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        _PreflightCache& cache);

    /**
    Table 15.1 dispatch of [MANT1988] for operands without intersections
    (disjoint, touching, A in B, B in A). Operand parts are merged into
    `outRes`, so the operands are emptied.
    @param inSolidA first operand
    @param inSolidB second operand
    @param outRes result solid
    @param op UNION, INTERSECTION or SUBTRACT
    @return `outRes`
    */
    static PolyhedralBoundedSolid* runSetOpNoIntersectionCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        PolyhedralBoundedSolid* outRes,
        int op);
    static PolyhedralBoundedSolid* runSetOpNoIntersectionCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        PolyhedralBoundedSolid* outRes,
        int op,
        _PreflightCache& cache);

    /**
    Witness test used to tell a right empty result from a lost solid
    (Fable5.2Analysis.md Stage F step 4): true when some point is interior
    to both solids, so their intersection cannot be empty. The operands
    must be intact (not consumed by a set operation).
    */
    static bool interiorsOverlap(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB);

    /**
    Witness test for A \ B: true when a vertex or a face interior witness
    of `solidA` lies strictly outside `solidB`.
    */
    static bool hasBoundaryPointOutside(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB);

    /**
    True when some vertex of `inner`, or a point inside one of its faces,
    lies on the boundary of `outer`. Meant for a solid already known to be
    contained in `outer`.
    */
    static bool touchesBoundaryFromInside(PolyhedralBoundedSolid* inner,
        PolyhedralBoundedSolid* outer);

    /**
    A point strictly inside the face (see the implementation).
    @return false when there is none (Java null)
    */
    static bool faceInteriorWitness(_PolyhedralBoundedSolidFace* face,
        Vector3Dd* out);

    /**
    True when a face of A and a face of B lie on the same plane with
    opposite orientation and share a region of positive area.
    */
    static bool hasOppositeCoplanarFaceAreaContact(
        PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB);

    /**
    Classifies `point` against `solid` (INSIDE, OUTSIDE or LIMIT), with the
    face snapshots `facePlanes` of `solid`.
    */
    static int classifyPointAgainstSolid(PolyhedralBoundedSolid* solid,
        const java::ArrayList<PolyhedralBoundedSolidFaceGeometry*>& facePlanes,
        const Vector3Dd& point);

private:
    typedef java::ArrayList<Vector3Dd> Polygon;

    /**
    Face snapshots of every face of a solid, indexed by face position,
    owned by this holder (the Java `precomputeFacePlanes` array). Only
    valid while the solid is not mutated.
    */
    class FacePlanes {
    public:
        explicit FacePlanes(PolyhedralBoundedSolid* solid);
        ~FacePlanes();
        const java::ArrayList<PolyhedralBoundedSolidFaceGeometry*>& list() const;
    private:
        java::ArrayList<PolyhedralBoundedSolidFaceGeometry*> planes;
        FacePlanes(const FacePlanes&);
        FacePlanes& operator=(const FacePlanes&);
    };

    static const int NO_INT_RELATION_DISJOINT = 0;
    static const int NO_INT_RELATION_TOUCHING = 1;
    static const int NO_INT_RELATION_A_IN_B = 2;
    static const int NO_INT_RELATION_B_IN_A = 3;

    _PolyhedralBoundedSolidSetNonIntersectingClassifier();

    static int compareToZero(double value);
    static int pointInFace(_PolyhedralBoundedSolidFace* face,
        const Vector3Dd& point);
    static void appendContactLaminae(PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB, PolyhedralBoundedSolid* outRes);
    static int classifyPointAgainstSolid(PolyhedralBoundedSolid* solid,
        const Vector3Dd& point);
    static bool hasConfirmedInteriorOverlap(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB);
    static bool isTangentlyContained(PolyhedralBoundedSolid* inner,
        PolyhedralBoundedSolid* outer);
    static bool someFaceWitnessIsOutside(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB,
        const java::ArrayList<PolyhedralBoundedSolidFaceGeometry*>& planesB);
    static bool witnessInsideFace(_PolyhedralBoundedSolidFace* source,
        _PolyhedralBoundedSolidFace* target);
    static double polygonArea(const Polygon& points);
    static int classifySolidAgainstSolid(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB);
    static int classifyNoIntersectionRelation(int aInB, int bInA);
    static bool hasProperEdgeFaceIntersection(PolyhedralBoundedSolid* current,
        PolyhedralBoundedSolid* other);
    static java::ArrayList<Polygon> partialCoplanarFaceAreaOverlapPolygons(PolyhedralBoundedSolid* solidA, PolyhedralBoundedSolid* solidB);
    static bool coplanarFaces(_PolyhedralBoundedSolidFace* faceA,
        _PolyhedralBoundedSolidFace* faceB);
    static bool partialCoplanarFaceAreaOverlap(
        _PolyhedralBoundedSolidFace* faceA,
        _PolyhedralBoundedSolidFace* faceB);
    static Polygon coplanarFaceIntersectionPolygon(
        _PolyhedralBoundedSolidFace* faceA,
        _PolyhedralBoundedSolidFace* faceB);
    static void appendFaceVerticesInsideOther(Polygon& points,
        _PolyhedralBoundedSolidFace* source,
        _PolyhedralBoundedSolidFace* target);
    static void appendBoundaryIntersections(Polygon& points,
        _PolyhedralBoundedSolidFace* faceA,
        _PolyhedralBoundedSolidFace* faceB);
    static void appendLoopIntersections(Polygon& points,
        _PolyhedralBoundedSolidLoop* loopA,
        _PolyhedralBoundedSolidLoop* loopB,
        int dominantCoordinate);
    static void appendSegmentIntersection(Polygon& points,
        _PolyhedralBoundedSolidHalfEdge* heA,
        _PolyhedralBoundedSolidHalfEdge* heB,
        int dominantCoordinate);
    static double cross2D(const Vector2Dd& a, const Vector2Dd& b);
    static void appendUniquePoint(Polygon& points, const Vector3Dd& point);
    static void sortCoplanarPolygon(Polygon& points, const Vector3Dd& normal);
    static double coplanarPolygonAreaMagnitude(const Polygon& points,
        const Vector3Dd& normal);
    static PolyhedralBoundedSolid* createLaminaFromPolygon(
        const Polygon& points);
    static bool faceHasInteriorVertexOrEdgeMidpoint(
        _PolyhedralBoundedSolidFace* source,
        _PolyhedralBoundedSolidFace* target);
    static bool faceBoundariesCrossProperly(
        _PolyhedralBoundedSolidFace* faceA,
        _PolyhedralBoundedSolidFace* faceB);
    static bool loopsCrossProperly(_PolyhedralBoundedSolidLoop* loopA,
        _PolyhedralBoundedSolidLoop* loopB, int dominantCoordinate);
    static int dominantCoordinateForFace(_PolyhedralBoundedSolidFace* face);
    static Vector2Dd projectPointTo2D(const Vector3Dd& in,
        int dominantCoordinate);
    static double orientation2D(const Vector2Dd& a, const Vector2Dd& b,
        const Vector2Dd& c);
    static bool segmentsCrossProperly2D(const Vector2Dd& a1,
        const Vector2Dd& a2, const Vector2Dd& b1, const Vector2Dd& b2);
};

#endif
