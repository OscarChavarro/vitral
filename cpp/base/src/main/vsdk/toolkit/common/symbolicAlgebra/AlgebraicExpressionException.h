#ifndef __ALGEBRAIC_EXPRESSION_EXCEPTION__
#define __ALGEBRAIC_EXPRESSION_EXCEPTION__

#include "java/lang/String.h"
#include <exception>
class AlgebraicExpressionException : public std::exception {
private:
    java::String message;

public:
    explicit AlgebraicExpressionException(const java::String& msg) : message(msg) {}

    const char* what() const noexcept override
    {
        return message.c_str();
    }
};

#endif
