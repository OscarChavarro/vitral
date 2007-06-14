//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 19 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.awt;

import java.awt.image.BufferedImage;

import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBPixel;

public class AwtImageRenderer
{
    /**
    Given an input BufferedImage, this method copies its contents to the
    specified output image. If output image currently exists, this method
    doesn't initialize its contents, but merely copies pixels. If the
    output image previously had a different size that the input's size,
    then is initialized.
    */
    public static boolean importRGBImageFromAwtBufferedImage(
        BufferedImage input, Image output
    )
    {
        int w = input.getWidth();
        int h = input.getHeight();
        int w2 = output.getXSize();
        int h2 = output.getYSize();

        if ( w != w2 || h != h2 ) {
            if ( !output.init(w, h) ) {
                return false;
            }
        }

        int x, y;
        int pixel;
        RGBPixel p = new RGBPixel();

        for ( y = 0; y < h; y++ ) {
            for ( x = 0; x < w; x++ ) {
                // Warning: This method call is so slow...
                pixel = input.getRGB(x, y);
                p.r = (byte)((pixel & 0x00FF0000) >> 16);
                p.g = (byte)((pixel & 0x0000FF00) >> 8);
                p.b = (byte)((pixel & 0x000000FF));
                output.putPixelRgb(x, y, p);
            }
        }
        return true;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
