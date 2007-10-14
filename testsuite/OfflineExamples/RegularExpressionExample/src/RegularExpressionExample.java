//===========================================================================
//= This example serves as a testbed for basic RGB image manipulation       =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - August 2 2006 - Oscar Chavarro: comments added                        =
//===========================================================================

// Basic JDK classes
import java.io.File;

// VSDK classes
import vsdk.toolkit.common.VSDK;                      // Utilities
import vsdk.toolkit.media.RGBImage;                   // Model elements
import vsdk.toolkit.io.image.ImagePersistence;        // Persistence elements

public class RegularExpressionExample 
{
    private static void performImageOperation1(RGBImage img)
    {
        int x, y;
        int xSize, ySize;
        byte r, g, b;

        xSize = img.getXSize();
        ySize = img.getYSize();
        for ( y = 0; y < ySize/2; y++ ) {
            for ( x = 0; x < xSize/2; x++ ) {
                r = VSDK.unsigned8BitInteger2signedByte(255);
                g = VSDK.unsigned8BitInteger2signedByte(0);
                b = VSDK.unsigned8BitInteger2signedByte(0);
                img.putPixel(x, y, r, g, b);
            }
        }
    }

    public static void main (String[] args) {
        RGBImage img = null;
        String imageFilename = "../../../etc/images/render.jpg";

        try {
            img = ImagePersistence.importRGB(new File(imageFilename));
        }
        catch (Exception e) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            System.exit(0);
        }

        performImageOperation1(img);
        ImagePersistence.exportJPG(new File("output.jpg"), img);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
