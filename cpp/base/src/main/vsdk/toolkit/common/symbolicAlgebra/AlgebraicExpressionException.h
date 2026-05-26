#ifndef __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSIONEXCEPTION_H__
#define __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSIONEXCEPTION_H__

#include <stdexcept>
#include "java/lang/String.h"
#include "java/lang/String.h"
#include "java/lang/String.h"

class AlgebraicExpressionException : public std::runtime_error {
public:
    explicit AlgebraicExpressionException(const java::String& msg) : std::runtime_error(msg) {}
};

#endif
