package vsdk.toolkit.common;
import java.io.Serial;

public abstract class _AlgebraicExpressionNode extends FundamentalEntity
{
    @Serial private static final long serialVersionUID = 20071014L;

    public abstract double eval() throws AlgebraicExpressionException;

    @Override
    public abstract String toString();
}
