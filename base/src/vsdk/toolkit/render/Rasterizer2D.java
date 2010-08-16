//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [BRES1965] Bresenham, J.E. "Algorithm for computer control of a digital =
//=            plotter" IBM Syst. J. 4, 1 (1965), 25-30.                    =
//===========================================================================

package vsdk.toolkit.render;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.Image;

public class Rasterizer2D
{
    /**
    This algorithm implements the Bresenham line algoritm with NO CLIPPING!
    See [BRES1965].
    Note that this is a currently naive implementation that makes use
    of double floating point arithmetic, while original Bresenham algorithm
    make use of most efficient integer line arithmetic.
    */
    public static void drawLine(Image img, int x0, int y0, int x1, int y1, RGBPixel p)
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
                if ( x >= 0 && x < img.getXSize() &&
                     y >= 0 && y < img.getYSize() ) {
                    img.putPixelRgb(x, y, p);
                }
                yy += dydx;
            }
          }
          else if ( Math.abs(dx) > VSDK.EPSILON && Math.abs(dy/dx) <= 1 && x1 < x0 ) {
            // Pendiente entre -1 y 1
            dydx = dy/dx;
            for ( x = x1, yy = (double)y1; x <= x0; x++ ) {
                y = (int)yy;
                if ( x >= 0 && x < img.getXSize() &&
                     y >= 0 && y < img.getYSize() ) {
                    img.putPixelRgb(x, y, p);
                }
                yy += dydx;
            }
          }
          else if ( Math.abs(dy) > VSDK.EPSILON && y1 > y0 ) {
            // Pendiente mayor a 1 o menor a -1
            dxdy = dx/dy;
            for ( y = y0, xx = (double)x0; y <= y1; y++ ) {
                x = (int)xx;
                if ( x >= 0 && x < img.getXSize() &&
                     y >= 0 && y < img.getYSize() ) {
                    img.putPixelRgb(x, y, p);
                }
                xx += dxdy;
            }
          }
          else if ( Math.abs(dy) > VSDK.EPSILON && y1 < y0 ) {
            // Pendiente mayor a 1 o menor a -1
            dxdy = dx/dy;
            for ( y = y1, xx = (double)x1; y <= y0; y++ ) {
                x = (int)xx;
                if ( x >= 0 && x < img.getXSize() &&
                     y >= 0 && y < img.getYSize() ) {
                    img.putPixelRgb(x, y, p);
                }
                xx += dxdy;
            }
          }
        ;
    }

}
