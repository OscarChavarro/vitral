//= This example serves as a testbed for basic RGB image manipulation       =

// Basic JDK classes
import java.io.File;

// VSDK classes
import vsdk.toolkit.common.VSDK;                      // Utilities
import vsdk.toolkit.media.RGBImageUncompressed;                   // Model elements
import vsdk.toolkit.io.image.ImagePersistence;        // Persistence elements

public class ImageOfflineExample 
{
    private static void performImageOperation1(RGBImageUncompressed img)
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
        RGBImageUncompressed img = null;
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
        ImagePersistence.exportBMP(new File("output.bmp"), img);
        System.out.println("Resulting image has been written to \"output.bmp\"");
    }

}
