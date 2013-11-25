//===========================================================================

package vsdk.toolkit.io.image;

// Java classes
import java.io.InputStream;

// VitralSDK classes
import vsdk.toolkit.media.RGBImage;

public class ImagePersistenceAndroid extends ImagePersistenceHelper
{
    public boolean jpgExportSupported()
    {
        return true;
    }

    public RGBImage importRGB(InputStream is) throws ImageNotRecognizedException, Exception
    {
        throw new Exception("Not supported helper");
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
