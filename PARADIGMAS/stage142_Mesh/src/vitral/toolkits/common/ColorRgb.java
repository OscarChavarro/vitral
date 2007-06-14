//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 15 2005 - David Diaz: Original base version                 =
//= - November 1 2005 - Oscar Chavarro: Quality check - comments added      =
//===========================================================================

package vitral.toolkits.common;

import java.text.DecimalFormat;
import java.text.FieldPosition;

/**
Respect to data representation:

The `r`, `g`, and `b` class attributes represent red, green and blue 
components in a color specification, with values in the range [0, inf) when
used in High Dinamic Range Imaginery (HDRI). Note that no restriction as been
specified regarding to units to be used, and as of this revision the units
must be application defined. When not used in HDRI, the values must be
application-clamped to the range [0, 1]. A value of 0 always will represent
'no contribution' or 'black', and a value of 1 will be 'white' in non HDRI
applications. Interpretation in HDRI applications is pending to be defined.

Note that the `r`, `g`, and `b` class attributes are PUBLIC, converting this
class in an not evolvable structure, and IT MUST BE KEEP AS IS, due to
performance issues in a lot of algorithms, as this avoids indirections.
Nevertheless, get and set methods are provided.
*/
public class ColorRgb
{
    /// Contains the red component of a color 
    public double r;
    public double g;
    public double b;

    /**
    Note that default assumed color in the toolkit is black. It is
    important to note that changing this default could impact some
    algorithms. Do not change it.
    */
    public ColorRgb()
    {
        r = 0;
        g = 0;
        b = 0;
    }

    /**
    This constructor builds a ColorRgb from another one.
    */
    public ColorRgb(ColorRgb c)
    {
        r = c.r;
        g = c.g;
        b = c.b;
    }

    /**
    This constructor builds a ColorRgb from individual component values.
    */
    public ColorRgb(double r, double g, double b)
    {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
    This method return a String representation of current color. In its
    current implementation it is biased for human readability, not for
    precision, so the use of an approximation formating.
    */
    public String toString()
    {
        return "<" + f(r) + ", " + f(g) + ", " + f(b) + ">";
    }

    /**
    Given a double number, it formats it to print in a given precision
    */
    private String f(double a)
    {
        DecimalFormat f = new DecimalFormat("0.##");

        return f.format(a, new StringBuffer(""), new FieldPosition(0)).toString();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
