package vsdk.toolkit.common.symbolicAlgebra;
import java.io.Serial;
import vsdk.toolkit.common.VSDK;

public class _AlgebraicExpressionConstantNode extends _AlgebraicExpressionNode
{
    @Serial private static final long serialVersionUID = 20071014L;

    private double val;
    public _AlgebraicExpressionConstantNode(double val)
    {
        this.val = val;
    }

    @Override
    public double eval() throws AlgebraicExpressionException
    {
        return val;
    }

    @Override
    public String toString()
    {
        String msg;

        msg = "" + VSDK.formatDouble(val);
        return msg;
    }
}
