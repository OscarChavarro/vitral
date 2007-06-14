//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 15 2005 - Oscar Chavarro: Original base version             =
//= - November 28 2005 - Oscar Chavarro: Quality check - comments added     =
//= - November 28 2005 - Oscar Chavarro: set/get methods added              =
//= - February 13 2005 - Oscar Chavarro: quality check                      =
//===========================================================================

package vsdk.toolkit.media;

import vsdk.toolkit.common.Entity;

/**
Respect to data representation:

The `r`, `g`, `b` and `a` class attributes represent red, green, blue and
alpha components in a color specification, with values in the range 
[0, 255], for use in color raster systems.

Note that the `r`, `g`, `b` and `a` class attributes are PUBLIC, converting 
this class in an not evolvable structure, and IT MUST BE KEEP AS IS, due to
performance issues in a lot of algorithms, as this avoids indirections.
Nevertheless, get and set methods are provided.
*/

public class RGBAPixel extends Entity {
    public byte r;
    public byte g;
    public byte b;
    public byte a;

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

    public void setA(byte a)
    {
        this.a = a;
    }

    public byte getA()
    {
        return a;
    }

    public String toString()
    {
        return "<" + r + ", " + g + ", " + b + " / (" + a + ")>";
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
