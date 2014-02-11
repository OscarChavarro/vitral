//===========================================================================
package vsdk.toolkit.render.androidgles20;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;

public class AndroidGLES20ImageRenderer extends AndroidGLES20Renderer
{
    public static int activate(Image img)
    {
        if ( img == null ) {
            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES20ImageRenderer.activate",
            "Trying to activate a NULL Image!");
	}

        if ( img instanceof RGBAImage ) {
            return AndroidGLES20RGBAImageRenderer.activate((RGBAImage)img);
        }
        else if ( img instanceof RGBImage ) {
            return AndroidGLES20RGBImageRenderer.activate((RGBImage)img);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES20ImageRenderer.activate",
            "Image GL activation not implemented for subclass " + c);
        }
        return -1;
    }

    /*
    public static void draw(Image img)
    {
        if ( img instanceof RGBAImage ) {
            AndroidGLES20RGBAImageRenderer.draw((RGBAImage)img);
        }
        else if ( img instanceof RGBImage ) {
            AndroidGLES20RGBImageRenderer.draw((RGBImage)img);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES20ImageRenderer.draw",
            "Image GL activation not implemented for subclass " + c);
        }
    }
    */
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
