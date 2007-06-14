//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 29 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.media;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.VSDK;

/**
This abstract class establishes the required interface for all Image classes
in the VSDK toolkit, and provides some common utilities for nearest and
bi-linear interpolation evaluation on Rgb space.
*/

public abstract class Image extends Entity
{
    /**
    Given the width and height of the desired new size for this image, this
    method is responsable of allocating the necesary memory to keep such
    an image.
    @param width - desired new width in pixels for the image. Must be greater
    than 0.
    @param height - desired new height in pixels for the image. Must be greater
    than 0.
    @return true if image memory could be allocated, false otherwise
    */
    public abstract boolean init(int width, int height);

    /**
    Returns current image width in pixels
    @return current image width in pixels
    */
    public abstract int getXSize();

    /**
    Returns current image height in pixels
    @return current image height in pixels
    */
    public abstract int getYSize();

    /**
    Given an image position inside its current boundaries and an RGBPixel,
    this method convert the pixel RGB value to its internal colorspace,
    and updates corresponding internal pixel value.
    @param x - x cooordinate of desired pixel, must be between 0 and image
    width minus 1
    @param y - y cooordinate of desired pixel, must be between 0 and image
    height minus 1
    */
    public abstract void putPixelRgb(int x, int y, RGBPixel p);

    /**
    Given an image position inside its current boundaries, an RGBPixel is
    returned from internal color space representation.
    @param x - x cooordinate of desired pixel, must be between 0 and image
    width minus 1
    @param y - y cooordinate of desired pixel, must be between 0 and image
    height minus 1
    @return the RGBPixel corresponding to requested pixel coordinate
    inside the image.
    */
    public abstract RGBPixel getPixelRgb(int x, int y);

    /**
    Given a double value inside the integer limits of this image, this
    method returns a rgb color corresponding to the nearest getPixelRgb.

    @todo: implement this method.
    */
    public ColorRgb getColorRgbNearest(double x, double y)
    {
        return new ColorRgb();
    }

    /**
    Given a double value inside the integer limits of this image, this
    method returns a rgb color corresponding to a bi-linear interpolation
    of the 4 neighboring pixels of the float position.

    @todo: implement this method.
    */
    public ColorRgb getColorRgbBiLinear(double x, double y)
    {
        return new ColorRgb();
    }

    /**
    This method creates a checker board like visual test pattern with centered
    and colored crossing lines. This is provided as a quick image builder in
    RGB space to test image algorithm.
    */
    public void createTestPattern()
    {
        int i;
        int j;
        RGBPixel p = new RGBPixel();

        for ( i = 0; i < getXSize(); i++ ) {
            for ( j = 0; j < getYSize(); j++ ) {
                if ( ((i % 2 != 0) && (j % 2 == 0)) || 
                     ((j % 2 != 0) && (i % 2 == 0)) ) {
                    p.r = (byte)255;
                    p.g = (byte)255;
                    p.b = (byte)255;
                  }
                  else {
                    p.r = 0;
                    p.g = 0;
                    p.b = 0;
                }
                if ( j == getYSize()/2 ) {
                    p.r = (byte)255;
                    p.g = 0;
                    p.b = 0;
                }
                if ( i == getXSize()/2) {
                    p.r = 0;
                    p.g = (byte)255;
                    p.b = 0;
                }
                putPixelRgb(i, j, p);
            }
        }
    }

    /**
    This algorithm implements the Bresenham line algoritm with NO CLIPPING!
    See [BRES1965].
    */
    public void drawLine(int x0, int y0, int x1, int y1, RGBPixel p)
    {
        double dx, dy;
        double dxdy;
        double dydx;
        int x, y;
        double xx, yy;

        dx = (double)(x1-x0);
        dy = (double)(y1-y0);

        if ( Math.abs(dx) > VSDK.EPSILON && Math.abs(dy/dx) <= 1 && x1 > x0 ) {
            // Pendiente entre -1 y 1
            dydx = dy/dx;
            for ( x = x0, yy = (double)y0; x <= x1; x++ ) {
                y = (int)yy;
                if ( x >= 0 && x < getXSize() &&
                     y >= 0 && y < getYSize() ) {
                    putPixelRgb(x, y, p);
                }
                yy += dydx;
            }
          }
          else if ( Math.abs(dx) > VSDK.EPSILON && Math.abs(dy/dx) <= 1 && x1 < x0 ) {
            // Pendiente entre -1 y 1
            dydx = dy/dx;
            for ( x = x1, yy = (double)y1; x <= x0; x++ ) {
                y = (int)yy;
                if ( x >= 0 && x < getXSize() &&
                     y >= 0 && y < getYSize() ) {
                    putPixelRgb(x, y, p);
                }
                yy += dydx;
            }
          }
          else if ( Math.abs(dy) > VSDK.EPSILON && y1 > y0 ) {
            // Pendiente mayor a 1 o menor a -1
            dxdy = dx/dy;
            for ( y = y0, xx = (double)x0; y <= y1; y++ ) {
                x = (int)xx;
                if ( x >= 0 && x < getXSize() &&
                     y >= 0 && y < getYSize() ) {
                    putPixelRgb(x, y, p);
                }
                xx += dxdy;
            }
          }
          else if ( Math.abs(dy) > VSDK.EPSILON && y1 < y0 ) {
            // Pendiente mayor a 1 o menor a -1
            dxdy = dx/dy;
            for ( y = y1, xx = (double)x1; y <= y0; y++ ) {
                x = (int)xx;
                if ( x >= 0 && x < getXSize() &&
                     y >= 0 && y < getYSize() ) {
                    putPixelRgb(x, y, p);
                }
                xx += dxdy;
            }
          }
        ;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
