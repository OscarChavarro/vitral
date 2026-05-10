package vsdk.toolkit.common.symbolicAlgebra;
import java.io.Serial;
import vsdk.toolkit.common.FundamentalEntity;

public abstract class _AlgebraicExpressionNode extends FundamentalEntity
{
    @Serial private static final long serialVersionUID = 20071014L;

    public abstract double eval() throws AlgebraicExpressionException;

    @Override
    public abstract String toString();
}
