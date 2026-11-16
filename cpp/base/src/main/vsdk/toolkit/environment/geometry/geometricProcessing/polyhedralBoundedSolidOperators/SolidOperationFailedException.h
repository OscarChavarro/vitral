#ifndef __SOLID_OPERATION_FAILED_EXCEPTION__
#define __SOLID_OPERATION_FAILED_EXCEPTION__

#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"

/**
The operation could not produce a result: an Euler operator reported a
fatal inconsistency (the cause is the original `VSDKFatalException`), the
pipeline returned no solid, or it returned an empty solid where the operands
prove that the result is not empty (for instance a union of non-empty
solids, or an intersection of solids whose interiors overlap). An empty
result that is right, such as A \ A, is returned normally; see
`PolyhedralBoundedSolidModeler::setOp`.
*/
class SolidOperationFailedException : public PolyhedralBoundedSolidOperationException
{
public:
    explicit SolidOperationFailedException(const java::String& message)
        : PolyhedralBoundedSolidOperationException(message) {}

    SolidOperationFailedException(const java::String& message, const java::Throwable& cause)
        : PolyhedralBoundedSolidOperationException(message, cause) {}

    virtual ~SolidOperationFailedException() {}
};

#endif
