import java.io.File;

import vsdk.toolkit.io.image.NativeImageReaderWrapper;
import vsdk.toolkit.io.image._NativeImageReaderWrapperHeaderInfo;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.io.image.ImagePersistence;

public class testImageReader
{
    public static void main(String args[])
    {
        _NativeImageReaderWrapperHeaderInfo header;
        header = new _NativeImageReaderWrapperHeaderInfo();
        try {
            NativeImageReaderWrapper.readPngHeader(header, "input.png");

            RGBImage img = new RGBImage();
            img.initNoFill((int)header.xSize, (int)header.ySize);

            System.out.printf("Processing image of %d x %d pixels... ", header.xSize, header.ySize);
            NativeImageReaderWrapper.readPngDataRGB(header, img.getRawImageDirectBuffer());
            System.out.println("Ok!");

            System.out.print("Exporting test image on PPM format... ");
            ImagePersistence.exportPPM(new File("./output.ppm"), img);
            System.out.println("Ok!");
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
