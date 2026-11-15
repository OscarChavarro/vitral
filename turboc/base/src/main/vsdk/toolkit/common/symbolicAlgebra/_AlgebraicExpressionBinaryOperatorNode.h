#ifndef __ALGBRC_EXPRSN_BNRY_OPRTR_ND__
#define __ALGBRC_EXPRSN_BNRY_OPRTR_ND__

#include "vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionNode.h"

class AlgebraicExpression;

class _AlgebraicExpressionBinaryOperatorNode : public _AlgebraicExpressionNode {
private:
    const AlgebraicExpression* parent;
    char operatorChar;
    _AlgebraicExpressionNode* leftOperand;
    _AlgebraicExpressionNode* rightOperand;

    _AlgebraicExpressionBinaryOperatorNode(
        const _AlgebraicExpressionBinaryOperatorNode& other);
    _AlgebraicExpressionBinaryOperatorNode& operator=(
        const _AlgebraicExpressionBinaryOperatorNode& other);

public:
    _AlgebraicExpressionBinaryOperatorNode(const AlgebraicExpression* parent,
                                           char op);
    virtual ~_AlgebraicExpressionBinaryOperatorNode();

    /**
    @param operand left operand, owned by this node
    */
    void setLeftOperand(_AlgebraicExpressionNode* operand);

    /**
    @param operand right operand, owned by this node
    */
    void setRightOperand(_AlgebraicExpressionNode* operand);
    virtual double eval() const;
    virtual String toString() const;
};

#endif
