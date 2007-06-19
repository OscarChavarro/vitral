// Basic JDK classes
import java.io.File;

// VSDK classes
import vsdk.toolkit.common.VSDK;                      // Utilities
import vsdk.toolkit.media.RGBImage;                   // Model elements
import vsdk.toolkit.io.image.ImagePersistence;        // Persistence elements

public class ImageOfflineExample 
{
    private static void performImageOperation1(RGBImage img)
    {
        int x, y;
        int xSize, ySize;

        xSize = img.getXSize();
        ySize = img.getYSize();
        for ( y = 0; y < ySize; y++ ) {
            for ( x = 0; x < xSize; x++ ) {
        if ( y % 2 == 0 ) {
                    img.putPixel(x, y, (byte)0, (byte)0, (byte)0);
        }
        else {
                    img.putPixel(x, y, (byte)-1, (byte)-1, (byte)-1);
        }
            }
        }
    }

    public static void main (String[] args) {
        RGBImage img = new RGBImage();
    img.init(640, 480);
        performImageOperation1(img);
        ImagePersistence.exportJPG(new File("output.jpg"), img);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
