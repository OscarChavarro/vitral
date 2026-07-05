#ifndef __ALGEBRAICEXPRESSION__
#define __ALGEBRAICEXPRESSION__

#include "java/lang/String.h"
#include "java/util/HashMap.h"
class AlgebraicExpression {
private:
    java::String expression;
    java::HashMap<java::String, double> vars;

public:
    void setExpression(const java::String& expr);
    void defineValue(const java::String& name, double value);
    double eval() const;
};

#endif
