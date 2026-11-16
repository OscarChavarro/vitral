#ifndef __INVALID_SOLID_RESULT_EXCEPTION__
#define __INVALID_SOLID_RESULT_EXCEPTION__

#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"

/**
The operation completed, but its result fails strict validation
(`PolyhedralBoundedSolidValidationEngine::validateStrict`): self-intersecting
faces, broken shells or a wrong Euler characteristic. The message carries
the operation, the result path and the validator report.
*/
class InvalidSolidResultException : public PolyhedralBoundedSolidOperationException
{
public:
    explicit InvalidSolidResultException(const java::String& message)
        : PolyhedralBoundedSolidOperationException(message) {}

    virtual ~InvalidSolidResultException() {}
};

#endif
