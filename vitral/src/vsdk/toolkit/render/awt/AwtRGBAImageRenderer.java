//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 28 2005 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.awt;

import java.awt.Color;
import java.awt.Graphics;

import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBAPixel;

public class AwtRGBAImageRenderer
{
    /** Converts integers in the domain [-128, 127] to integers in the range
    [0, 255] */
    private static int scaleValue(byte x)
    {
        int y;

        y = (int)x;
        if (y < 0) y = 256 + y;

        return y;
    }

    public static void draw(Graphics dc, RGBAImage img, int x0, int y0)
    {
        int x, y;
        RGBAPixel pixel;

        for ( y = 0; y < img.getYSize(); y++ ) {
            for ( x = 0; x < img.getXSize(); x++ ) {
                pixel = img.getPixel(x, y);
                dc.setColor(
                    new Color( scaleValue(pixel.r), 
                               scaleValue(pixel.g), 
                               scaleValue(pixel.b) )
                );
                dc.drawLine(x+x0, y+y0, x+x0, y+y0);
            }
        }
    }

    public static void draw(Graphics dc, RGBAImage img)
    {
        draw(dc, img, 0, 0);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
