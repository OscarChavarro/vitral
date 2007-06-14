//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 15 2005 - Oscar Chavarro: Original base version             =
//= - February 2006 - Oscar Chavarro: set/get methods added                 =
//===========================================================================

package vitral.toolkits.media;

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

public class RGBPixel {
    public byte r;
    public byte g;
    public byte b;
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
