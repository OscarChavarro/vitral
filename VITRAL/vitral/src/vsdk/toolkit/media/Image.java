//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 29 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.media;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.ColorRgb;

/**
This abstract class establishes the required interface for all Image classes
in the vsdk toolkit, and provides some common utilities for nearest and
bi-linear interpolation evaluation on Rgb space.
*/

public abstract class Image extends Entity
{
    public abstract boolean init(int width, int height);
    public abstract int getXSize();
    public abstract int getYSize();
    public abstract void putPixelRgb(int x, int y, RGBPixel p);
    public abstract RGBPixel getPixelRgb(int x, int y);

    /**
    Given a double value inside the integer limits of this image, this
    method returns a rgb color corresponding to the nearest getPixelRgb.

    TODO: implement this method.
    */
    public ColorRgb getColorRgbNearest(double x, double y)
    {
        return new ColorRgb();
    }

    /**
    Given a double value inside the integer limits of this image, this
    method returns a rgb color corresponding to a bi-linear interpolation
    of the 4 neighboring pixels of the float position.

    TODO: implement this method.
    */
    public ColorRgb getColorRgbBiLinear(double x, double y)
    {
        return new ColorRgb();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
