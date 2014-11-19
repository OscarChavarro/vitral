//===========================================================================
package vsdk.toolkit.render.androidgles10;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;

public class AndroidGLES10ImageRenderer extends AndroidGLES10Renderer
{
    public static int activate(Image img)
    {
        if ( img == null ) {
            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES10ImageRenderer.activate",
            "Trying to activate a NULL Image!");
        }

        if ( img instanceof RGBAImage ) {
            return AndroidGLES10RGBAImageRenderer.activate((RGBAImage)img);
        }
        else if ( img instanceof RGBImage ) {
            return AndroidGLES10RGBImageRenderer.activate((RGBImage)img);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES20ImageRenderer.activate",
            "Image GL activation not implemented for subclass " + c);
        }
        return -1;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
