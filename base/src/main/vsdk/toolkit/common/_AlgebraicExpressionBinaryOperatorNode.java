package vsdk.toolkit.common;

import java.io.Serial;

public class _AlgebraicExpressionBinaryOperatorNode extends _AlgebraicExpressionNode
{
    @Serial
    private static final long serialVersionUID = 20071014L;

    private AlgebraicExpression parent;
    private char operator;
    private _AlgebraicExpressionNode leftOperand;
    private _AlgebraicExpressionNode rightOperand;

    public _AlgebraicExpressionBinaryOperatorNode(AlgebraicExpression parent, char op)
    {
        this.parent = parent;
        operator = op;
    }

    public void setLeftOperand(_AlgebraicExpressionNode operand)
    {
        this.leftOperand = operand;
    }

    public void setRightOperand(_AlgebraicExpressionNode operand)
    {
        this.rightOperand = operand;
    }

    @Override
    public double eval() throws AlgebraicExpressionException
    {
        double lval = leftOperand.eval();
        double rval = rightOperand.eval();
        double val = Double.NaN;

        switch( operator ) {
          case '+':    val = lval + rval;    break;
          case '-':    val = lval - rval;    break;
          case '*':    val = lval * rval;    break;
          case '/':    val = lval / rval;    break;
          case '^':    val = Math.pow(lval, rval);    break;
          default:
            throw new AlgebraicExpressionException("Unknown binary operator \"" + operator + "\"");
        }
        return val;
    }

    @Override
    public String toString()
    {
        String msg;

        msg = "(" + leftOperand.toString() + ") " + operator + " (" + rightOperand.toString() + ")";
        return msg;
    }
}
