#ifndef __PBS_OPERATION_FAILED_EXC__
#define __PBS_OPERATION_FAILED_EXC__

#include "java/lang/String.h"
#include "java/lang/Throwable.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"

/**
The operation could not produce a result: an Euler operator reported a
fatal inconsistency (the cause is the original `VSDKFatalException`), the
pipeline returned no solid, or it returned an empty solid where the operands
prove that the result is not empty (for instance a union of non-empty
solids, or an intersection of solids whose interiors overlap). An empty
result that is right, such as A \ A, is returned normally; see
`PolyhedralBoundedSolidModeler::setOp`.

C++ port note: `Throwable` keeps no cause, so the constructor with a cause
only keeps the message.
*/
class SolidOperationFailedException : public PolyhedralBoundedSolidOperationException {
public:
    explicit SolidOperationFailedException(const String& message) :
        PolyhedralBoundedSolidOperationException(message) {}

    SolidOperationFailedException(const String& message, const Throwable& /*cause*/) :
        PolyhedralBoundedSolidOperationException(message) {}

    virtual ~SolidOperationFailedException() {}
};

#endif
