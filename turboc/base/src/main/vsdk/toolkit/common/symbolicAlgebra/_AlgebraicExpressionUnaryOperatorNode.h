#ifndef __ALGBRC_EXPRSN_UNRY_OPRTR_ND__
#define __ALGBRC_EXPRSN_UNRY_OPRTR_ND__

#include "vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionNode.h"

class AlgebraicExpression;

class _AlgebraicExpressionUnaryOperatorNode : public _AlgebraicExpressionNode {
private:
    const AlgebraicExpression* parent;
    String operatorName;
    _AlgebraicExpressionNode* operand;

    _AlgebraicExpressionUnaryOperatorNode(
        const _AlgebraicExpressionUnaryOperatorNode& other);
    _AlgebraicExpressionUnaryOperatorNode& operator=(
        const _AlgebraicExpressionUnaryOperatorNode& other);

public:
    _AlgebraicExpressionUnaryOperatorNode(const AlgebraicExpression* parent,
                                          const String& operatorName);
    virtual ~_AlgebraicExpressionUnaryOperatorNode();

    /**
    @param operand operand of the operator, owned by this node
    */
    void setOperand(_AlgebraicExpressionNode* operand);
    virtual double eval() const;
    virtual String toString() const;
};

#endif
