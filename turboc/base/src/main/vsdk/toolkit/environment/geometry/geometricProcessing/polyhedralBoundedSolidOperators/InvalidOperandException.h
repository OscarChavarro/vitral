#ifndef __PBS_INVALID_OPERAND_EXC__
#define __PBS_INVALID_OPERAND_EXC__

#include "java/lang/String.h"
#include "java/lang/Throwable.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"

/**
An operand of a boolean operation is not a valid boundary model
(`PolyhedralBoundedSolidValidationEngine::validateBooleanInputs` fails), even
after coincident vertices were welded. The operation does not start: the
pipeline of [MANT1988] Ch. 15 assumes valid operands. The message carries
the validator report.

C++ port note: `Throwable` keeps no cause, so the constructor with a cause
only keeps the message.
*/
class InvalidOperandException : public PolyhedralBoundedSolidOperationException {
public:
    explicit InvalidOperandException(const String& message) :
        PolyhedralBoundedSolidOperationException(message) {}

    InvalidOperandException(const String& message, const Throwable& /*cause*/) :
        PolyhedralBoundedSolidOperationException(message) {}

    virtual ~InvalidOperandException() {}
};

#endif
