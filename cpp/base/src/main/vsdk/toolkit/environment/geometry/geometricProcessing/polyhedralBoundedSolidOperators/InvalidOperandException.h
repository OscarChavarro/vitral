#ifndef __INVALID_OPERAND_EXCEPTION__
#define __INVALID_OPERAND_EXCEPTION__

#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"

/**
An operand of a boolean operation is not a valid boundary model
(`PolyhedralBoundedSolidValidationEngine::validateBooleanInputs` fails), even
after coincident vertices were welded. The operation does not start: the
pipeline of [MANT1988] Ch. 15 assumes valid operands. The message carries
the validator report.
*/
class InvalidOperandException : public PolyhedralBoundedSolidOperationException
{
public:
    explicit InvalidOperandException(const java::String& message)
        : PolyhedralBoundedSolidOperationException(message) {}

    InvalidOperandException(const java::String& message, const java::Throwable& cause)
        : PolyhedralBoundedSolidOperationException(message, cause) {}

    virtual ~InvalidOperandException() {}
};

#endif
