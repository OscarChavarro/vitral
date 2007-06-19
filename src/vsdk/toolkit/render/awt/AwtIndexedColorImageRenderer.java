//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 18 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.awt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.media.RGBPixel;

public class AwtIndexedColorImageRenderer extends AwtRenderer
{
    public static void draw(Graphics dc, IndexedColorImage img, int x0, int y0)
    {
        int x, y;
        RGBPixel pixel;

        for ( y = 0; y < img.getYSize(); y++ ) {
            for ( x = 0; x < img.getXSize(); x++ ) {
                pixel = img.getPixelRgb(x, y);
                dc.setColor(
                    new Color( VSDK.signedByte2unsignedInteger(pixel.r), 
                               VSDK.signedByte2unsignedInteger(pixel.g), 
                               VSDK.signedByte2unsignedInteger(pixel.b) )
                );
                dc.drawLine(x+x0, y+y0, x+x0, y+y0);
            }
        }
    }

    public static void draw(Graphics dc, IndexedColorImage img)
    {
        draw(dc, img, 0, 0);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
