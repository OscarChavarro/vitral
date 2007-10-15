//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 14 2007 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.common;

import vsdk.toolkit.common.VSDK;

public class _RegularExpressionConstantNode extends _RegularExpressionNode
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20071014L;

    private double val;
    public _RegularExpressionConstantNode(double val)
    {
        this.val = val;
    }

    public double eval() throws RegularExpressionException
    {
        return val;
    }

    public String toString()
    {
        String msg;

        msg = "" + VSDK.formatDouble(val);
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
