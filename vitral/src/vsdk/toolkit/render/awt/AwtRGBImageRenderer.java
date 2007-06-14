//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 19 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.awt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;

public class AwtRGBImageRenderer
{
    public static void draw(Graphics dc, RGBImage img, int x0, int y0)
    {
        int x, y;
        RGBPixel pixel;

        for ( y = 0; y < img.getYSize(); y++ ) {
            for ( x = 0; x < img.getXSize(); x++ ) {
                pixel = img.getPixel(x, y);
                dc.setColor(
                    new Color( VSDK.signedByte2unsignedInteger(pixel.r), 
                               VSDK.signedByte2unsignedInteger(pixel.g), 
                               VSDK.signedByte2unsignedInteger(pixel.b) )
                );
                dc.drawLine(x+x0, y+y0, x+x0, y+y0);
            }
        }
    }

    public static void draw(Graphics dc, RGBImage img)
    {
        draw(dc, img, 0, 0);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
