#ifndef __POLYHEDRAL_BOUNDED_SOLID_OPERATION_EXCEPTION__
#define __POLYHEDRAL_BOUNDED_SOLID_OPERATION_EXCEPTION__

#include "java/lang/IllegalStateException.h"
#include "java/lang/String.h"
#include "java/lang/Throwable.h"

/**
Base of the typed failures raised by the public operations of
`PolyhedralBoundedSolidModeler` (set operations and split). It extends
`IllegalStateException` so that callers written against the earlier
untyped failures keep working (Fable5.2Analysis.md Stage F).

- `NonManifoldBooleanResultException`: the regularized result would be
  non-manifold and is rejected by design.
- `InvalidSolidResultException`: the operation completed but its result
  fails the strict B-Rep postcondition.
- `SolidOperationFailedException`: the operation could not produce a
  result (an internal Euler-operator failure, or a result that lost the
  solid, such as an empty union).

With the non-destructive entry points the operands are intact after any
of these failures.

C++ port note: the emulated `java::Throwable` keeps no cause; the
constructor that takes one keeps only the message.
*/
class PolyhedralBoundedSolidOperationException
    : public java::IllegalStateException
{
public:
    explicit PolyhedralBoundedSolidOperationException(
        const java::String& message)
        : java::IllegalStateException(message) {}

    PolyhedralBoundedSolidOperationException(const java::String& message,
        const java::Throwable& cause)
        : java::IllegalStateException(message) { (void)cause; }

    virtual ~PolyhedralBoundedSolidOperationException() {}
};

#endif
