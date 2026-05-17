#ifndef __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSIONEXCEPTION_H__
#define __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSIONEXCEPTION_H__

#include <stdexcept>
#include <string>

class AlgebraicExpressionException : public std::runtime_error {
public:
    explicit AlgebraicExpressionException(const std::string& msg) : std::runtime_error(msg) {}
};

#endif
