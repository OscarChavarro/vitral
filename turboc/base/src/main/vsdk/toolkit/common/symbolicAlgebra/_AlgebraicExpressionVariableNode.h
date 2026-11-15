#ifndef __ALGBRC_EXPRSSN_VRBL_ND__
#define __ALGBRC_EXPRSSN_VRBL_ND__

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
