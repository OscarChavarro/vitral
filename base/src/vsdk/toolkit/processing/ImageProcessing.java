//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 30 2007 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.processing;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
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
    Given the `input` and `output` previously created images, fills in
    `output`'s space the `this` image using bilinear interpolation.
    @todo worked well only in the growing case. Must add the shrinking
    case for area averaging.
    */
    public static void resize(Image input, Image output)
    {
        int xSize = output.getXSize();
        int ySize = output.getYSize();
        double u, v;
        int x, y;
        ColorRgb source;
        RGBPixel target = new RGBPixel();

        if ( xSize >= input.getXSize() && ySize >= input.getYSize() ) {
            for ( x = 0; x < xSize; x++ ) {
                for ( y = 0; y < ySize; y++ ) {
                    u = ((double)x)/((double)(xSize));
                    v = ((double)y)/((double)(ySize));
                    source = input.getColorRgbBiLinear(u, v);
                    target.r = VSDK.unsigned8BitInteger2signedByte((int)(source.r*255));
                    target.g = VSDK.unsigned8BitInteger2signedByte((int)(source.g*255));
                    target.b = VSDK.unsigned8BitInteger2signedByte((int)(source.b*255));
                    output.putPixelRgb(x, y, target);
                }
            }
	}
	else {
	    output.createTestPattern();
	}
    }

    /**
    A distance field is a scalar map where each pixel value correspond to the
    nearest distance to an "inside" pixel.
    Every pixel in the input image with a value greater or equal to `threshold`
    will be noted as "inside", otherwise will be "outside".
    This implements the naive, real, full, simple (direct) and non-optimized
    version of the algorithm, which doesn't have extra memory requirements
    and has the following complexity:
       - Time: O(N^4)
       - Space: O(2*N^2)
    Where N is the size in pixels of a squared input image for the square
    image case.
    This version of the algorithm is provided for reference (comparison between
    this algorithm results and optimized versions' results). Its use is not
    recommended for applications' use. Use processDistanceFieldWithArray
    instead.
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

    /**
    A distance field is a scalar map where each pixel value correspond to the
    nearest distance to an "inside" pixel.
    Every pixel in the input image with a value greater or equal to `threshold`
    will be noted as "inside", otherwise will be "outside".
    This implements an optimized version of the algorithm in method
    `processDistanceField`. Current optimization was made using a dynamic
    programming technique which requires an extra preprocessing step and
    and array, which is of N^2 positions in the worst case.
    Algorithm with optimization is bounded by
       - Time: O(N^4)
       - Space: O(3*N^2)
    but falls to
       - Time: O((2+K)*N^2)
       - Space: O(2*N^2+K)
    where K is usually N*0.06 in contour type images.

    Where N is the size in pixels of a squared input image for the square
    image case.
    This version of the algorithm is provided for reference (comparison between
    this algorithm results and optimized versions' results). Its use is not
    recommended for applications' use. Use processDistanceFieldWithArray
    instead.
    */
    public static boolean
    processDistanceFieldWithArray(Image inInput, IndexedColorImage outOutput,
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

        int x, y;
        int arrSize = 0;
        RGBPixel p;
        int val;

        //- Preprocessing phase 1: determine number of zero distance pixels
        for ( x = 0; x < dx; x++ ) {
            for ( y = 0; y < dy; y++ ) {
                p = inInput.getPixelRgb(x, y);
                val = (VSDK.signedByte2unsignedInteger(p.r) +
                       VSDK.signedByte2unsignedInteger(p.g) +
                       VSDK.signedByte2unsignedInteger(p.b)) / 3;
                if ( val >= threshold ) {
                    arrSize++;
                }
            }
        }

        //- Preprocessing phase 2: fill array with 0-distance pixel coords.
        int xcoords[];
        int ycoords[];
        int i = 0;

        xcoords = new int[arrSize];
        ycoords = new int[arrSize];
        for ( x = 0; x < dx; x++ ) {
            for ( y = 0; y < dy; y++ ) {
                p = inInput.getPixelRgb(x, y);
                val = (VSDK.signedByte2unsignedInteger(p.r) +
                       VSDK.signedByte2unsignedInteger(p.g) +
                       VSDK.signedByte2unsignedInteger(p.b)) / 3;
                if ( val >= threshold ) {
                    xcoords[i] = x;
                    ycoords[i] = y;
                    i++;
                }
            }
        }

        //- Optimized distance field algorithm ----------------------------
        int xx, yy;
        double dist2;
        double maxdist2 = ((double)dx)*((double)dx) + ((double)dy)*((double)dy);
        double mindist2;
        double maxdist = Math.sqrt(maxdist2);

        for ( x = 0; x < dx; x++ ) {
            for ( y = 0; y < dy; y++ ) {
                mindist2 = maxdist2;
                for ( i = 0; i < arrSize; i++ ) {
                    xx = xcoords[i];
                    yy = ycoords[i];
                    dist2 = 
                      ((double)xx - (double)x) * ((double)xx - (double)x) +
                      ((double)yy - (double)y) * ((double)yy - (double)y);
                    if ( dist2 < mindist2 ) {
                        mindist2 = dist2;
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
