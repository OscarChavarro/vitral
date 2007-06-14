//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 30 2007 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.processing;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBPixel;

/**
@todo Current implementation is not well designed. This class' design should
be checked to inforce:
  - Interoperability with existing image processing toolkits/frameworks like
    JAI, ITK, Khoros, OpenCV, Matlab+ImageToolbox, ImageMagick+JMagick, GIMP,
    etc.
  - Programmability of image processing operations using GPUs
  - Filter graph approach
*/

public abstract class ImageProcessing extends ProcessingElement {

    private static int
    gammaCorrection8bits(int in, double gamma)
    {
        double a, b;
    int out;

    a = ((double)in) / 255.0;
    b = Math.pow(a, 1.0/gamma);
    out = (int)(b*255.0);

    return out;
    }

    public static void
    gammaCorrection(IndexedColorImage img, double gamma)
    {
        int x, y;
    int val;

        for ( x = 0; x < img.getXSize(); x++ ) {
            for ( y = 0; y < img.getYSize(); y++ ) {
        val = img.getPixel(x, y);
        val = gammaCorrection8bits(val, gamma);
        img.putPixel(x, y, VSDK.unsigned8BitInteger2signedByte(val));
        }
    }
    }

    /**
    A distance field is a scalar map where each pixel value correspond to the
    nearest distance to an "inside" pixel.
    Every pixel in the input image with a value greater or equal to `threshold`
    will be noted as "inside", otherwise will be "outside".
    */
    public static boolean
    processDistanceField(Image inInput, IndexedColorImage outOutput,
        int threshold)
    {
        if ( inInput == null || outOutput == null ) {
            return false;
        }

        int dx = inInput.getXSize();
        int dy = inInput.getYSize();

        if ( dx != outOutput.getXSize() || dy != outOutput.getYSize() ) {
            return false;
        }

        int x, y, xx, yy;
        RGBPixel p;
        boolean pixel = false;
        double dist2;
        double maxdist2 = ((double)dx)*((double)dx) + ((double)dy)*((double)dy);
        double mindist2;
        double maxdist = Math.sqrt(maxdist2);
        int val;

        for ( x = 0; x < dx; x++ ) {
            for ( y = 0; y < dy; y++ ) {
                // Calculate the nearest distance to output (x, y)
                mindist2 = maxdist2;

                for ( xx = 0; xx < dx; xx++ ) {
                    for ( yy = 0; yy < dy; yy++ ) {
                        p = inInput.getPixelRgb(xx, yy);
                        val = (VSDK.signedByte2unsignedInteger(p.r) +
                               VSDK.signedByte2unsignedInteger(p.g) +
                               VSDK.signedByte2unsignedInteger(p.b)) / 3;
        
                        if ( val >= threshold ) {
                            dist2 = 
                         ((double)xx - (double)x) * ((double)xx - (double)x) +
                         ((double)yy - (double)y) * ((double)yy - (double)y);

                            if ( dist2 < mindist2 ) {
                                mindist2 = dist2;
                            }
                        }
                    }
                }

                // Set output value to current mindistance
                val = (int)((Math.sqrt(mindist2) / maxdist)*255.0);
                outOutput.putPixel(x, y,
                    VSDK.unsigned8BitInteger2signedByte(val));
            }
        }

        return true;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
