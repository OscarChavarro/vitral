#ifndef __ALGEBRAIC_EXPRESSION_EXCEPTION__
#define __ALGEBRAIC_EXPRESSION_EXCEPTION__

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
