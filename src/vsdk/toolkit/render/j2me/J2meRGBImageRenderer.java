//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 29 2007 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.j2me;

// J2ME Classes
import javax.microedition.lcdui.Graphics;

// VitralSDK Classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;

public class J2meRGBImageRenderer extends J2meRenderer
{
    public static void draw(Graphics g, RGBImage img, int x0, int y0)
    {
        RGBPixel p;
        int rr, gg, bb;
        int x, y;
        int scanline[];

        scanline = new int[img.getXSize()];
        for ( y = 0; y < img.getYSize(); y++ ) {
            for ( x = 0; x < img.getXSize(); x++ ) {
                p = img.getPixel(x, y);
                rr = VSDK.signedByte2unsignedInteger(p.r);
                gg = VSDK.signedByte2unsignedInteger(p.g);
                bb = VSDK.signedByte2unsignedInteger(p.b);
                scanline[x] = 0xFF000000 + (rr << 16) + (gg << 8) + (bb);
            }
            g.drawRGB(scanline, 0, img.getXSize(), x0, y0+y, img.getXSize(), 1, false);
        }

        /*
        // This is soooo slowwww....
        for ( x = 0; x < img.getXSize(); x += 1 ) {
            for ( y = 0; y < img.getYSize(); y += 1 ) {
                p = img.getPixel(x, y);
                g.setColor(VSDK.signedByte2unsignedInteger(p.r), 
                           VSDK.signedByte2unsignedInteger(p.g), 
                           VSDK.signedByte2unsignedInteger(p.b));
                g.drawLine(x, y, x, y);
            }
        }
        */

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
