//===========================================================================

package vsdk.toolkit.io.image;

// Java classes
import java.io.InputStream;

// VitralSDK classes
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;

public class ImagePersistenceAndroid extends ImagePersistenceHelper
{
    public boolean rgbFormatFromInputStreamSupported(String fileExtension)
    {
        return true;
    }

    public boolean rgbaFormatFromInputStreamSupported(String fileExtension)
    {
        return true;
    }

    public RGBImage importRGB(InputStream is) throws ImageNotRecognizedException, Exception
    {
        RGBImage img;
        img = new RGBImage();
        img.init(320, 240);
        img.createTestPattern();

        return img;
    }

    public RGBAImage importRGBA(InputStream is) throws ImageNotRecognizedException, Exception
    {
        RGBAImage img;
        img = new RGBAImage();
        img.init(320, 240);
        img.createTestPattern();

        return img;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
