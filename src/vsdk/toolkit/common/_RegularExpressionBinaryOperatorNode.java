//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 14 2007 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.common;

public class _RegularExpressionBinaryOperatorNode extends _RegularExpressionNode
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20071014L;

    private RegularExpression parent;
    private char operator;
    private _RegularExpressionNode leftOperand;
    private _RegularExpressionNode rightOperand;

    public _RegularExpressionBinaryOperatorNode(RegularExpression parent, char op)
    {
        this.parent = parent;
        operator = op;
    }

    public void setLeftOperand(_RegularExpressionNode operand)
    {
        this.leftOperand = operand;
    }

    public void setRightOperand(_RegularExpressionNode operand)
    {
        this.rightOperand = operand;
    }

    public double eval() throws RegularExpressionException
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
            throw new RegularExpressionException("Unknown binary operator \"" + operator + "\"");
	}
        return val;
    }

    public String toString()
    {
        String msg;

        msg = "(" + leftOperand.toString() + ") " + operator + " (" + rightOperand.toString() + ")";
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
