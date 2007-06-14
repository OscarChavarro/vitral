//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 15 2005 - Oscar Chavarro: Original base version             =
//= - March 14 2006 - Oscar Chavarro: set/get methods added              =
//===========================================================================

package vsdk.toolkit.media;

import vsdk.toolkit.common.Entity;

/**
Respect to data representation:

The `r`, `g`, and `b` class attributes represent red, green and blue
components in a color specification, with values in the range 
[0, 255], for use in color raster systems.

Note that the `r`, `g` and `b` class attributes are PUBLIC, converting 
this class in an not evolvable structure, and IT MUST BE KEEP AS IS, due to
performance issues in a lot of algorithms, as this avoids indirections.
Nevertheless, get and set methods are provided.
*/

public class RGBPixel extends Entity {
    public byte r;
    public byte g;
    public byte b;

    public void setR(byte r)
    {
        this.r = r;
    }

    public byte getR()
    {
        return r;
    }

    public void setG(byte g)
    {
        this.g = g;
    }

    public byte getG()
    {
        return g;
    }

    public void setB(byte b)
    {
        this.b = b;
    }

    public byte getB()
    {
        return b;
    }

    public String toString()
    {
        return "<" + r + ", " + g + ", " + b + ">";
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
