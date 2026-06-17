#ifndef __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSIONEXCEPTION_H__
#define __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSIONEXCEPTION_H__

#include "java/lang/String.h"
#include <exception>
class AlgebraicExpressionException : public std::exception {
private:
    java::String message_;

public:
    explicit AlgebraicExpressionException(const java::String& msg) : message_(msg) {}

    const char* what() const noexcept override
    {
        return message_.c_str();
    }
};

#endif
