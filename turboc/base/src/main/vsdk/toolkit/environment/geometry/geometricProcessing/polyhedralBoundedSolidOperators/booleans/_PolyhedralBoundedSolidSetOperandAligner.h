//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_ST_PRND_LGNR__
#define ___PBS_ST_PRND_LGNR__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;

/**
The one decision of the set operations on whether two faces coincide.

A face of B that is parallel to a face of A, next to it, and closer
than two tolerances is, for the kernel, the same plane. Left as it is, each
stage decides that on its own: generate with the distance of each vertex,
the classifiers with the planes, the contact decision with interior
points. Inside that band they disagree: one finds a crossing where another
finds a contact, the intersection curve does not close, and the operation
is rejected (51 of the 432 operations of the near-degenerate gate,
plan-stage11).

The decision is taken here, once, before any stage runs: B is
translated, as a rigid body, by the least displacement that puts those
faces on the planes of their partners. After it every stage sees exact
coincidence, which it handles. A translation keeps every face of B planar
and every angle; moving single vertices would not. The displacement is at
most two tolerances per direction, the size the kernel already takes for
zero.

When the pairs ask for displacements that cannot all hold (two faces of
B close to two parallel faces of A at different distances), nothing is
moved and the operands go on as they are.

This works on the copies of the operands that the operation owns. Only
B is moved, so A u B and B u A can differ by that displacement.

C++ counterpart of Java's `_PolyhedralBoundedSolidSetOperandAligner`.
*/
class _PolyhedralBoundedSolidSetOperandAligner
    : public _PolyhedralBoundedSolidOperator {
public:
    /**
    Moves `solidB` so that its faces inside the band of a parallel face of
    `solidA` lie on that face's plane.
    C++ port note: Java returns the displacement or null; here the
    displacement goes to `outDisplacement` and the result tells whether
    something was moved.
    @return true when `solidB` was moved
    */
    static BOOLEAN align(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB, Vector3Dd& outDisplacement);

#ifdef __TURBOC__
public:
#else
private:
#endif
    _PolyhedralBoundedSolidSetOperandAligner();
    static BOOLEAN firstVertex(_PolyhedralBoundedSolidFace* face,
        Vector3Dd& out);
    static Vector3Dd unit(const Vector3Dd& vector);
};

#endif
