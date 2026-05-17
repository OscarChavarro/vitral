#ifndef __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSION_H__
#define __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSION_H__

#include <map>
#include <string>

class AlgebraicExpression {
private:
    std::string expression;
    std::map<std::string, double> vars;

public:
    void setExpression(const std::string& expr);
    void defineValue(const std::string& name, double value);
    double eval() const;
};

#endif
