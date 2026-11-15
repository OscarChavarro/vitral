#ifndef __ALGBRC_EXPRSSN_CNSTNT_ND__
#define __ALGBRC_EXPRSSN_CNSTNT_ND__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionNode.h"

class _AlgebraicExpressionConstantNode : public _AlgebraicExpressionNode {
private:
    double val;

public:
    explicit _AlgebraicExpressionConstantNode(double val);
    virtual double eval() const;
    virtual String toString() const;
};

#endif
