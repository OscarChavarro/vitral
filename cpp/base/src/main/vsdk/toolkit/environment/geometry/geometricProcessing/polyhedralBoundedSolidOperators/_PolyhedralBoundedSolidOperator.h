//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___POLYHEDRAL_BOUNDED_SOLID_OPERATOR__
#define ___POLYHEDRAL_BOUNDED_SOLID_OPERATOR__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/processing/ProcessingElement.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidIdNamespace;
class _PolyhedralBoundedSolidLoop;

/**
Shared low-level services for B-Rep operators. This class is not intended to
be used directly; it provides common behavior for slicing and boolean
operators.

C++ counterpart of Java's `_PolyhedralBoundedSolidOperator`.
*/
class _PolyhedralBoundedSolidOperator : public ProcessingElement {
public:
    static const int UNION = 1;
    static const int INTERSECTION = 2;
    static const int SUBTRACT = 3;
    static const int DIFFERENCE = SUBTRACT;

protected:
    static PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext;

    /**
    @param context tolerances of the current operation, or null for the
    default ones
    */
    static void setNumericContext(
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext* context);

    /** Identifiers of the current set operation, or null (referenced) */
    static _PolyhedralBoundedSolidIdNamespace* idNamespace;

    static void setIdNamespace(_PolyhedralBoundedSolidIdNamespace* ns);

    /**
    Following section [MANT1988].14.7.1 and program [MANT1988].14.8.
    */
    static bool neighbor(_PolyhedralBoundedSolidHalfEdge* h1,
                         _PolyhedralBoundedSolidHalfEdge* h2);

    /**
    This is the answer to problem [MANT1988].14.2.

    \todo  Check for consistency of `emanatingHalfEdge` pointers for vertices.
    */
    static void cleanup(PolyhedralBoundedSolid* s);

    /**
    Following section [MANT1988].14.8. and program [MANT1988].14.12.
    */
    static void movefac(_PolyhedralBoundedSolidFace* f,
                        PolyhedralBoundedSolid* s);

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    Answer to problem [MANT1988].14.1.

    Current implementation assumes the following interpretation:
    Given a vertex of interest `he.startingVertex`, one can measure the angle
    of incidence of loop `he.parentLoop` on vertex of interest by measuring the
    angle between the halfedges `he` (direction `a`) and `he.previous`
    (direction `b`).  The bisector vector is the one having its tail on the
    vertex of interest position `he.startingVertex.position` and its end
    pointing in the middle of `a` and `b` directions.

    \todo : check current assumptions!

    This protected method is here for exclusive use of subclasses
    `_PolyhedralBoundedSolidSplitter` and `_PolyhedralBoundedSolidSetOperator`.
    */
    static Vector3Dd bisector(_PolyhedralBoundedSolidHalfEdge* he);

    /**
    Following section [MANT1988].14.7.2. and program [MANT1988].14.10.
    */
    static void join(_PolyhedralBoundedSolidHalfEdge* h1,
                     _PolyhedralBoundedSolidHalfEdge* h2, bool withDebug);

    /**
    This method checks whether the edges `he.previous().parentEdge` and
    `he.parentEdge` make a convex (less than 180 degrees) or concave
    (larger than 180 degrees) angle. In the first case the method returns
    `false` and `true` for the second case.
    This is an answer to problem [MANT1988].13.6.
    Current implementation intentionally follows the legacy boolean-kernel
    predicate: the sector is wide when the cross product of its two boundary
    vectors is degenerate or points opposite to the parent face normal.

    PRE: Parent solid should be previously validated to contain correct
    face equations.

    This protected method is here for exclusive use of subclasses
    `_PolyhedralBoundedSolidSplitter` and `_PolyhedralBoundedSolidSetOperator`.
    */
    static bool checkWideness(_PolyhedralBoundedSolidHalfEdge* he);

private:
    /**
    The outer loop of a face as `clearRingSide` needs it: its vertices in
    order and the coordinate dropped to project it. Built once per face, so
    that testing many rings against the same division walks the loop and
    computes the plane once (performance.md P-04). The answers are those of
    `outerLoopClearance` and `outerLoopParityContains`.
    */
    class _OuterLoopShadow
    {
      private:
        /** False when the loop has no start half-edge or is not closed. */
        bool walkable;
        /** False when the face has no plane. */
        bool planar;
        int dropAxis;
        Vector3Dd* points;
        int pointsLength;
        double* u;
        double* v;
        _OuterLoopShadow(const _OuterLoopShadow& other);
        _OuterLoopShadow& operator=(const _OuterLoopShadow& other);
      public:
        explicit _OuterLoopShadow(_PolyhedralBoundedSolidFace* f);
        ~_OuterLoopShadow();
        double clearance(const Vector3Dd& point) const;
        int contains(const Vector3Dd& point) const;
    };

    /**
    `clearRingSide` for many rings of the same division: the two outer
    loops and the tolerance are prepared once. Moving a ring from one face
    to the other does not change their outer loops, so one instance serves
    a whole pass over the rings.
    */
    class _RingSideTest
    {
      private:
        _PolyhedralBoundedSolidFace* firstFace;
        _PolyhedralBoundedSolidFace* secondFace;
        _OuterLoopShadow* shadow1;
        _OuterLoopShadow* shadow2;
        double tolerance;
        _RingSideTest(const _RingSideTest& other);
        _RingSideTest& operator=(const _RingSideTest& other);
      public:
        _RingSideTest(_PolyhedralBoundedSolidFace* f1,
                      _PolyhedralBoundedSolidFace* f2);
        ~_RingSideTest();
        int side(_PolyhedralBoundedSolidLoop* l);
    };

    static bool canMoveFace(_PolyhedralBoundedSolidFace* f);
    static void laringmv(_PolyhedralBoundedSolidFace* f1,
                         _PolyhedralBoundedSolidFace* f2);
    static void laringmv(_PolyhedralBoundedSolidFace* f1,
                         _PolyhedralBoundedSolidFace* f2,
                         _PolyhedralBoundedSolidLoop* keep);
    static int clearRingSide(_PolyhedralBoundedSolidFace* f1,
                             _PolyhedralBoundedSolidFace* f2,
                             _PolyhedralBoundedSolidLoop* l);
    static double outerLoopClearance(_PolyhedralBoundedSolidFace* f,
                                     const Vector3Dd& point);
    static _PolyhedralBoundedSolidLoop* holeLeftByDividedRing(
        _PolyhedralBoundedSolidFace* oldf,
        _PolyhedralBoundedSolidFace* newf,
        _PolyhedralBoundedSolidLoop* dividedInnerRing);
    static bool isWoundAsDisk(_PolyhedralBoundedSolidLoop* loop,
                              _PolyhedralBoundedSolidFace* face);
    static bool isWoundAsHole(_PolyhedralBoundedSolidLoop* loop,
                              _PolyhedralBoundedSolidFace* face);
    static double windingDot(_PolyhedralBoundedSolidLoop* loop,
                             _PolyhedralBoundedSolidFace* face);
    static bool ringBelongsToOtherHalf(_PolyhedralBoundedSolidFace* f1,
                                       _PolyhedralBoundedSolidFace* f2,
                                       _PolyhedralBoundedSolidLoop* l);
    static int outerLoopParityContains(_PolyhedralBoundedSolidFace* f,
                                       const Vector3Dd& point);
    static void traceRingMoveShadowDecision(_PolyhedralBoundedSolidFace* f1,
                                            _PolyhedralBoundedSolidFace* f2,
                                            _PolyhedralBoundedSolidLoop* l,
                                            const java::String& site);
    static double shadowProjectedU(const Vector3Dd& p, int dropAxis);
    static double shadowProjectedV(const Vector3Dd& p, int dropAxis);
};

#endif
