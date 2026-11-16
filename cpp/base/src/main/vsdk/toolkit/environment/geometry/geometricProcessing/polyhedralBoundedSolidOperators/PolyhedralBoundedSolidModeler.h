#ifndef __POLYHEDRAL_BOUNDED_SOLID_MODELER__
#define __POLYHEDRAL_BOUNDED_SOLID_MODELER__

#include "java/lang/IllegalStateException.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/processing/ProcessingElement.h"

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/concurrent/locks/ReentrantLock.h"

class InfinitePlane;
class ParametricCurve;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;

/**
Utility class with static modeling and boolean operations specific to
`PolyhedralBoundedSolid`.

This class contains creation/sweep/split/set-op helpers and centralizes the
polyhedral B-Rep operations previously exposed through `GeometricModeler`.

C++ port notes: `setOp` returns a new solid owned by the caller and leaves
the operands unchanged (it works on copies, released before returning);
`setOpDestructive` consumes the operands, which stay owned by the caller.
The Java `synchronized ( KERNEL_LOCK )` blocks are a recursive
`java::ReentrantLock` (never released, as the Java static object) held
by a guard.
*/
class PolyhedralBoundedSolidModeler : public ProcessingElement {
public:
    static const int UNION = 1;
    static const int INTERSECTION = 2;
    static const int SUBTRACT = 3;

    /**
    Applies a transformation matrix to all vertices in the solid.
    @param solid target solid instance.
    @param transformation transformation matrix to apply.
    */
    static void applyTransformation(PolyhedralBoundedSolid* solid,
                                    const Matrix4x4d& transformation);

    /**
    Rotational sweep of an open wire profile around the X axis, following the
    construction style of [MANT1988] 12.2 and 12.5.

    PRE:
    - `solid` is a wire-like profile (single face, open loop)
    - profile lies on `z = 0`
    - `numberOfFaces >= 3`
    */
    static void rotationalSweepExtrudeWireAroundXAxis(
        PolyhedralBoundedSolid* solid, int numberOfFaces);

    /**
    Builds a planar B-Rep face (with holes) from the contours of a curve.
    @param curve contours, separated by `BREAK` markers
    @return a new solid, owned by the caller
    */
    static PolyhedralBoundedSolid* createBrepFromParametricCurve(
        ParametricCurve* curve);

    /**
    Wrapper over `_PolyhedralBoundedSolidSplitter::split`, serialized on
    the kernel lock.

    Splits `inSolid` by `inSplittingPlane` and appends resulting pieces to
    `outSolidsAbove` and `outSolidsBelow` (owned by the caller; see
    `_PolyhedralBoundedSolidSplitter::split`).
    @throws SolidOperationFailedException when the split fails
    */
    static void split(PolyhedralBoundedSolid* inSolid,
                      const InfinitePlane& inSplittingPlane,
                      java::ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
                      java::ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow);

    /**
    Regularized set operation ([MANT1988] Ch. 15) with configurable strict
    result validation.

    The operands are not modified: the operation runs on structural copies
    of them ([MANT1988] Sec. 15.8, `scopy`). Use `setOpDestructive` to skip
    the copies when the operands are no longer needed; it consumes them as
    the book's algorithm does.

    Strict validation performs global shell/Euler analysis and an
    all-face-pairs intersection scan. It is enabled by default by the
    shorter overloads; callers can pass false here for an explicit
    performance/legacy-compatibility opt-out.

    Empty results. A solid with no faces is returned when the regularized
    result is empty (A \ A, the intersection of disjoint or touching
    solids, A \ B with A inside B); `isEmptyResult` tells it apart. When the
    operands prove that the result cannot be empty and the pipeline
    returned nothing, SolidOperationFailedException is raised instead of
    silently returning an empty solid.

    The public operations of this class are serialized on one lock: the
    kernel still keeps per-call state in static fields, so concurrent calls
    from several threads run one after the other.

    @return the result, owned by the caller
    @throws NonManifoldBooleanResultException when the result would be
        non-manifold; raised even when `doStrictValidation` is false
    @throws InvalidSolidResultException when a completed boolean result
        fails the strict B-Rep postcondition
    @throws SolidOperationFailedException when the operation fails or lost
        the solid
    */
    static PolyhedralBoundedSolid* setOp(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        int op,
        bool withDebug = false,
        bool maximizeResultFaces = true,
        bool doStrictValidation = true);

    /**
    Destructive form of `setOp`: no copies are made and the operands are
    consumed (still owned by the caller, holding fragments). The
    empty-result witness check is not done.
    */
    static PolyhedralBoundedSolid* setOpDestructive(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        int op,
        bool withDebug,
        bool maximizeResultFaces,
        bool doStrictValidation);

    /**
    True for the empty solid a set operation or a split returns when the
    regularized result is empty: no faces at all.
    */
    static bool isEmptyResult(PolyhedralBoundedSolid* result);

    /**
    Reports which path produced the last `setOp` result on the calling
    thread.
    @return null before the first call or when the last call failed before
    producing a result
    */
    static const java::String* getLastSetOpResultPath();

private:
    static java::ReentrantLock* const KERNEL_LOCK;

    static PolyhedralBoundedSolid* runSetOp(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        int op,
        bool withDebug,
        bool maximizeResultFaces,
        bool doStrictValidation);
    static bool resultCannotBeEmpty(PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB, int op);
    static java::String operationName(int op);
};

#endif
