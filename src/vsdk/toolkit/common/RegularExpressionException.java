//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 14 2007 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.common;

public class RegularExpressionException extends VSDKException
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20071014L;

    public RegularExpressionException()
    {
        ;
    }

    public RegularExpressionException(String message)
    {
        super(message);
    }

    public RegularExpressionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public RegularExpressionException(Throwable cause)
    {
        super(cause);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
