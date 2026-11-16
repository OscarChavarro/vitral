#ifndef __NON_MANIFOLD_BOOLEAN_RESULT_EXCEPTION__
#define __NON_MANIFOLD_BOOLEAN_RESULT_EXCEPTION__

#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"

/**
Thrown by the set operations when the regularized result would be a
non-manifold solid that the half-edge structure cannot represent as one
valid B-Rep: two shells that touch only along an edge or at a vertex (for
instance the union of two boxes sharing one edge, defect B-04 in
Fable5.2Analysis.md).

[MANT1988] Sec. 15.2 (criterion 3') would admit such a pseudomanifold
result; this kernel rejects it by design so that every returned solid passes
strict validation. Callers that need the two pieces can keep the operands.

Unlike `InvalidSolidResultException`, raised by the strict postcondition,
this failure is detected before any operand is modified and is raised
whether or not strict validation was requested.
*/
class NonManifoldBooleanResultException : public PolyhedralBoundedSolidOperationException
{
public:
    explicit NonManifoldBooleanResultException(const java::String& message)
        : PolyhedralBoundedSolidOperationException(message) {}

    virtual ~NonManifoldBooleanResultException() {}
};

#endif
