//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_SET_NON_INTERSECTING_CLASSIFIER__
#define ___PBS_SET_NON_INTERSECTING_CLASSIFIER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"

#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"

class InfinitePlane;
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
    runPartialCoplanarFaceAreaCase, runTouchingOnlyPreflightCase,
    runContainmentOnlyPreflightCase and runSetOpNoIntersectionCase. The cache
    does not change any decision.
    */
    // Keep this nested name short: Turbo C++ 3.0 truncates long C++ public
    // names while mangling them, which otherwise makes the AB and BA methods
    // collide in the object module.
    class Pfc {
    public:
        Pfc(PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b);

        int aInB();
        int bInA();
        BOOLEAN hasInteriorOverlap();
        BOOLEAN hasEfa();
        BOOLEAN hasEfb();

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
    static Pfc newPreflightCache(
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
    static BOOLEAN runContainmentOnlyPreflightCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB);
    static BOOLEAN runContainmentOnlyPreflightCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        Pfc& cache);

    /**
    Detects solids that only touch each other (no interior overlap, no proper
    edge/face crossing and no partial coplanar face overlap).
    @param inSolidA first operand
    @param inSolidB second operand
    @return true when the no-intersection dispatch applies
    */
    static BOOLEAN runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB);
    static BOOLEAN runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        Pfc& cache);

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
        Pfc& cache);

    /**
    Resolves operands that only share partial coplanar face areas: the
    intersection yields laminas over the contact polygons and the
    subtraction keeps `inSolidA`.
    @param inSolidA first operand
    @param inSolidB second operand
    @param outRes result solid
    @param op UNION, INTERSECTION or SUBTRACT
    @return `outRes`, or null when the case does not apply
    */
    static PolyhedralBoundedSolid* runPartialCoplanarFaceAreaCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        PolyhedralBoundedSolid* outRes,
        int op);
    static PolyhedralBoundedSolid* runPartialCoplanarFaceAreaCase(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        PolyhedralBoundedSolid* outRes,
        int op,
        Pfc& cache);

#ifdef __TURBOC__
public:
#else
private:
#endif
    typedef ArrayList<Vector3Dd> Polygon;

    /**
    Containing planes of every face of a solid, indexed by face position,
    owned by this holder. Only valid while the solid is not mutated.
    */
    class FacePlanes {
    public:
        explicit FacePlanes(PolyhedralBoundedSolid* solid);
        ~FacePlanes();
        InfinitePlane* get(long int i) const;
    private:
        ArrayList<InfinitePlane*> planes;
        FacePlanes(const FacePlanes&);
        FacePlanes& operator=(const FacePlanes&);
    };

    enum {
        NO_INT_RELATION_DISJOINT = 0,
        NO_INT_RELATION_TOUCHING = 1,
        NO_INT_RELATION_A_IN_B = 2,
        NO_INT_RELATION_B_IN_A = 3
    };

    _PolyhedralBoundedSolidSetNonIntersectingClassifier();

    static int compareToZero(double value);
    static int pointInFace(_PolyhedralBoundedSolidFace* face,
        const Vector3Dd& point);
    static int classifyPointAgainstSolid(PolyhedralBoundedSolid* solid,
        const FacePlanes& facePlanes, const Vector3Dd& point);
    static ArrayList<double> overlappingBounds(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB);
    static BOOLEAN hasPositiveOverlapVolume(const ArrayList<double>& bounds);
    static BOOLEAN hasInteriorOverlapWitnessInAabb(
        const ArrayList<double>& bounds,
        PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB,
        const FacePlanes& planesA,
        const FacePlanes& planesB);
    static ArrayList<double> axisProbeCoordinates(double min, double max);
    static double vertexCoordinate(_PolyhedralBoundedSolidVertex* vertex,
        int axis);
    static void appendInteriorVertexCoordinates(ArrayList<double>& coords,
        PolyhedralBoundedSolid* solid, int axis, double min, double max,
        double eps);
    static void appendUniqueInteriorSample(ArrayList<double>& samples,
        double value, double min, double max, double eps);
    static ArrayList<double> sampleCoordinates(double min, double max,
        PolyhedralBoundedSolid* solidA, PolyhedralBoundedSolid* solidB,
        int axis);
    static BOOLEAN hasConfirmedInteriorOverlap(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB);
    static int classifySolidAgainstSolid(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB);
    static int classifyNoIntersectionRelation(int aInB, int bInA);
    static BOOLEAN hasProperEdgeFaceIntersection(PolyhedralBoundedSolid* current,
        PolyhedralBoundedSolid* other);
    static BOOLEAN hasPartialCoplanarFaceAreaOverlap(
        PolyhedralBoundedSolid* solidA, PolyhedralBoundedSolid* solidB);
    static ArrayList<Polygon> partialCoplanarFaceAreaOverlapPolygons(
        PolyhedralBoundedSolid* solidA, PolyhedralBoundedSolid* solidB);
    static BOOLEAN coplanarFaces(_PolyhedralBoundedSolidFace* faceA,
        _PolyhedralBoundedSolidFace* faceB);
    static BOOLEAN partialCoplanarFaceAreaOverlap(
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
    static BOOLEAN faceHasInteriorVertexOrEdgeMidpoint(
        _PolyhedralBoundedSolidFace* source,
        _PolyhedralBoundedSolidFace* target);
    static BOOLEAN faceBoundariesCrossProperly(
        _PolyhedralBoundedSolidFace* faceA,
        _PolyhedralBoundedSolidFace* faceB);
    static BOOLEAN loopsCrossProperly(_PolyhedralBoundedSolidLoop* loopA,
        _PolyhedralBoundedSolidLoop* loopB, int dominantCoordinate);
    static int dominantCoordinateForFace(_PolyhedralBoundedSolidFace* face);
    static Vector2Dd projectPointTo2D(const Vector3Dd& in,
        int dominantCoordinate);
    static double orientation2D(const Vector2Dd& a, const Vector2Dd& b,
        const Vector2Dd& c);
    static BOOLEAN segmentsCrossProperly2D(const Vector2Dd& a1,
        const Vector2Dd& a2, const Vector2Dd& b1, const Vector2Dd& b2);
};

#endif
