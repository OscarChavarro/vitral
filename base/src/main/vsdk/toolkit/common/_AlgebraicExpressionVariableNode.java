package vsdk.toolkit.common;
import java.io.Serial;

public class _AlgebraicExpressionVariableNode extends _AlgebraicExpressionNode
{
    @Serial private static final long serialVersionUID = 20071014L;

    private AlgebraicExpression parent;
    private String name;

    public _AlgebraicExpressionVariableNode(AlgebraicExpression parent, String name)
    {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public double eval() throws AlgebraicExpressionException
    {
        double val = parent.getVariableValue(name);
        return val;
    }

    @Override
    public String toString()
    {
        String msg;

        msg = name;
        return msg;
    }

}
