//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 15 2005 - Oscar Chavarro: Original base version             =
//= - November 28 2005 - Oscar Chavarro: Quality check - comments added     =
//= - November 28 2005 - Oscar Chavarro: set/get methods added              =
//= - February 13 2005 - Oscar Chavarro: quality check                      =
//= - May 25 2006 - David Diaz / Oscar Chavarro: Documentation added        =
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
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    /// The red component of this RGBAPixel
    public byte r;

    /// The green component of this RGBAPixel
    public byte g;

    /// The blue component of this RGBAPixel
    public byte b;

    /** 
    Provided for testing purposes and object oriented programming style
    to access the `r` attribute. If performance is needed, prefer direct
    attribute use. 
    */
    public void setR(byte r)
    {
        this.r = r;
    }

    /** 
    Provided for testing purposes and object oriented programming style
    to access the `r` attribute. If performance is needed, prefer direct
    attribute use. 
    */
    public byte getR()
    {
        return r;
    }

    /** 
    Provided for testing purposes and object oriented programming style
    to access the `g` attribute. If performance is needed, prefer direct
    attribute use. 
    */
    public void setG(byte g)
    {
        this.g = g;
    }

    /** 
    Provided for testing purposes and object oriented programming style
    to access the `g` attribute. If performance is needed, prefer direct
    attribute use. 
    */
    public byte getG()
    {
        return g;
    }

    /** 
    Provided for testing purposes and object oriented programming style
    to access the `b` attribute. If performance is needed, prefer direct
    attribute use. 
    */
    public void setB(byte b)
    {
        this.b = b;
    }

    /** 
    Provided for testing purposes and object oriented programming style
    to access the `b` attribute. If performance is needed, prefer direct
    attribute use. 
    */
    public byte getB()
    {
        return b;
    }

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    */
    public String toString()
    {
        return "<" + r + ", " + g + ", " + b + ">";
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
