package vsdk.toolkit.common.symbolicAlgebra;
import java.io.Serial;
import vsdk.toolkit.common.VSDKException;

public class AlgebraicExpressionException extends VSDKException
{
    @Serial private static final long serialVersionUID = 20071014L;

    public AlgebraicExpressionException()
    {

    }

    public AlgebraicExpressionException(String message)
    {
        super(message);
    }

    public AlgebraicExpressionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public AlgebraicExpressionException(Throwable cause)
    {
        super(cause);
    }
}
