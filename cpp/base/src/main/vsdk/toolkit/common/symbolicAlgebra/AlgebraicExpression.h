#ifndef __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSION_H__
#define __VSDK_TOOLKIT_COMMON_SYMBOLICALGEBRA_ALGEBRAICEXPRESSION_H__

#include <map>
#include "java/lang/String.h"
#include "java/lang/String.h"
#include "java/lang/String.h"

class AlgebraicExpression {
private:
    java::String expression;
    std::map<java::String, double> vars;

public:
    void setExpression(const java::String& expr);
    void defineValue(const java::String& name, double value);
    double eval() const;
};

#endif
