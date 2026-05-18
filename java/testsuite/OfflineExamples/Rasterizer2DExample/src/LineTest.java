// Basic JDK classes
import java.io.File;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.render.Rasterizer2D;

public class LineTest
{
    public static void main (String[] args) {
        String outputFileName = (args != null && args.length > 0 && args[0] != null && !args[0].isBlank())
            ? args[0]
            : "output1.png";
        //-----------------------------------------------------------------
        RGBImageUncompressed img = null;
        RGBPixel color = new RGBPixel();

        color.r = -1;
        color.g = 0;
        color.b = 0;

        img = new RGBImageUncompressed();
        img.init(640, 480);
        //img.createTestPattern();

        //-----------------------------------------------------------------
        int x;
        int y;
        double a;

        for ( a = 0; a < 360.0; a += 15.0 ) {
            x = 320 + (int)(200.0*Math.cos(Math.toRadians(a)));
            y = 240 + (int)(200.0*Math.sin(Math.toRadians(a)));
            Rasterizer2D.drawLine(img, 320, 240, x, y, color);
        }

        //-----------------------------------------------------------------
        ImagePersistence.exportPNG(new File(outputFileName), img);
        System.out.println("Resulting image has been written to \"" + outputFileName + "\"");
    }

}
