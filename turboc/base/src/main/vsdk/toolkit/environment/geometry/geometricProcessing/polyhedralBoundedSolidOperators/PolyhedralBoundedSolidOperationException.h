#ifndef __PBS_OPERATION_EXCEPTION__
#define __PBS_OPERATION_EXCEPTION__

#include "java/lang/String.h"
#include "java/lang/Throwable.h"
#include "java/lang/IllegalStateException.h"

/**
Base of the typed failures raised by the public operations of
`PolyhedralBoundedSolidModeler` (set operations and split). It extends
`IllegalStateException` so that callers written against the earlier
untyped failures keep working (Fable5.2Analysis.md Stage F).
  - `NonManifoldBooleanResultException`: the regularized result
    would be non-manifold and is rejected by design.
  - `InvalidSolidResultException`: the operation completed but its
    result fails the strict B-Rep postcondition.
  - `SolidOperationFailedException`: the operation could not
    produce a result (an internal Euler-operator failure, or a result
    that lost the solid, such as an empty union).
With the non-destructive entry points the operands are intact after any
of these failures.

C++ port note: `Throwable` keeps no cause, so the constructor with a cause
only keeps the message.
*/
class PolyhedralBoundedSolidOperationException : public IllegalStateException {
public:
    explicit PolyhedralBoundedSolidOperationException(const String& message) :
        IllegalStateException(message) {}

    PolyhedralBoundedSolidOperationException(const String& message, const Throwable& /*cause*/) :
        IllegalStateException(message) {}

    virtual ~PolyhedralBoundedSolidOperationException() {}
};

#endif
