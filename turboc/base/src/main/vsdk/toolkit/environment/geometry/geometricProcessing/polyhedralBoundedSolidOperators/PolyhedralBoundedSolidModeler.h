#ifndef __PLYHDRL_BNDD_SLD_MDLR__
#define __PLYHDRL_BNDD_SLD_MDLR__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/processing/ProcessingElement.h"

#include "java/util/ArrayList.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/String.h"

class InfinitePlane;
class ParametricCurve;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;

/**
Utility class with static modeling and boolean operations specific to
`PolyhedralBoundedSolid`.

This class contains creation/sweep/split/set-op helpers and centralizes the
polyhedral B-Rep operations previously exposed through `GeometricModeler`.

C++ port note: `setOp` returns a new solid owned by the caller; the
operands are not modified and stay owned by the caller.
*/
class PolyhedralBoundedSolidModeler : public ProcessingElement {
public:
    enum {
        UNION = 1,
        INTERSECTION = 2,
        SUBTRACT = 3
    };

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
    Convenience wrapper over `_PolyhedralBoundedSolidSplitter::split`.

    Splits `inSolid` by `inSplittingPlane` and appends resulting pieces to
    `outSolidsAbove` and `outSolidsBelow` (owned by the caller; see
    `_PolyhedralBoundedSolidSplitter::split`).
    */
    static void split(PolyhedralBoundedSolid* inSolid,
                      const InfinitePlane& inSplittingPlane,
                      ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
                      ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow);

    /**
    Regularized set operation ([MANT1988] Ch. 15) with configurable strict
    result validation.

    The operands are not modified: the operation runs on structural
    copies of them ([MANT1988] Sec. 15.8, `scopy`). Use `setOpDestructive`
    to skip the copies when the operands are no longer needed; it consumes
    them as the book's algorithm does.

    Strict validation performs global shell/Euler analysis and an
    all-face-pairs intersection scan. It is enabled by default; callers can
    pass false for an explicit performance/legacy-compatibility opt-out.

    Empty results. A solid with no faces is returned when the regularized
    result is empty (A \ A, the intersection of disjoint or touching
    solids, A \ B with A inside B); `isEmptyResult` tells it apart.
    When the operands prove that the result cannot be empty (a union of
    non-empty solids, an intersection of solids whose interiors overlap,
    A \ B with a point of A strictly outside B) and the pipeline returned
    nothing, `SolidOperationFailedException` is raised instead of
    silently returning an empty solid.

    The public operations of this class are serialized on one lock: the
    kernel still keeps per-call state in static fields, so concurrent calls
    from several threads run one after the other.

    @param inSolidA first operand (not modified, owned by the caller)
    @param inSolidB second operand (not modified, owned by the caller)
    @param op UNION, INTERSECTION or SUBTRACT
    @param withDebug true to dump the pipeline state
    @param maximizeResultFaces true to maximize the result faces
    @param doStrictValidation true to validate the result strictly
    @return the result, owned by the caller
    @throws NonManifoldBooleanResultException when the result would be
        non-manifold (a union of solids touching only along edges or at
        vertices); raised even when `doStrictValidation` is false
    @throws InvalidSolidResultException when a completed boolean result
        fails the strict B-Rep postcondition
    @throws SolidOperationFailedException when the operation fails or lost
        the solid
    */
    static PolyhedralBoundedSolid* setOp(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        int op,
        BOOLEAN withDebug = FALSE,
        BOOLEAN maximizeResultFaces = TRUE,
        BOOLEAN doStrictValidation = TRUE);

    /**
    Destructive form of `setOp`: no copies are made and the operands are
    consumed ([MANT1988] Sec. 15.8: "the parts of A and B not required in
    the final result will remain in the original solids"). After the call
    the operands hold fragments and must not be used (they stay owned by
    the caller, who deletes them). The empty-result witness check is not
    done, since it needs intact operands.
    */
    static PolyhedralBoundedSolid* setOpDestructive(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        int op,
        BOOLEAN withDebug = FALSE,
        BOOLEAN maximizeResultFaces = TRUE,
        BOOLEAN doStrictValidation = TRUE);

    /**
    True for the empty solid a set operation or a split returns when the
    regularized result is empty: no faces at all.
    */
    static BOOLEAN isEmptyResult(PolyhedralBoundedSolid* result);

    /**
    Reports which path produced the last `setOp` result: "normal-pipeline"
    when the [MANT1988] Ch. 15 pipeline answered, otherwise the name of the
    preflight that did (for example "touching-only-preflight" or
    "no-intersection-after-classify").
    @return the path, or NULL (Java null) before the first call or when the
    last call failed before producing a result
    */
    static const String* getLastSetOpResultPath();

private:
    static PolyhedralBoundedSolid* runSetOp(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        int op,
        BOOLEAN withDebug,
        BOOLEAN maximizeResultFaces,
        BOOLEAN doStrictValidation);
    static BOOLEAN resultCannotBeEmpty(PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB, int op);
    static String operationName(int op);
};

#endif
