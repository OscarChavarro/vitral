//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 28 2005 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.awt;

import java.awt.Color;
import java.awt.Graphics;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBAPixel;

public class AwtRGBAImageRenderer
{
    public static void draw(Graphics dc, RGBAImage img, int x0, int y0)
    {
        int x, y;
        RGBAPixel pixel;

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

    public static void draw(Graphics dc, RGBAImage img)
    {
        draw(dc, img, 0, 0);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
