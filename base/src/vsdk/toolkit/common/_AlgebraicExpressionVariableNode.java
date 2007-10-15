//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 14 2007 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.common;

public class _RegularExpressionVariableNode extends _RegularExpressionNode
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20071014L;

    private RegularExpression parent;
    private String name;

    public _RegularExpressionVariableNode(RegularExpression parent, String name)
    {
        this.parent = parent;
        this.name = new String(name);
    }

    public double eval() throws RegularExpressionException
    {
        double val = parent.getVariableValue(name);
        return val;
    }

    public String toString()
    {
        String msg;

        msg = name;
        return msg;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
