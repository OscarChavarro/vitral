#ifndef __ALGEBRAIC_EXPRESSION_VARIABLE_NODE__
#define __ALGEBRAIC_EXPRESSION_VARIABLE_NODE__

#include "vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionNode.h"

class AlgebraicExpression;

class _AlgebraicExpressionVariableNode : public _AlgebraicExpressionNode {
private:
    const AlgebraicExpression* parent;
    String name;

public:
    _AlgebraicExpressionVariableNode(const AlgebraicExpression* parent,
                                     const String& name);
    virtual double eval() const;
    virtual String toString() const;
};

#endif
