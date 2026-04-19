package vsdk.toolkit.common;
import java.io.Serial;

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
