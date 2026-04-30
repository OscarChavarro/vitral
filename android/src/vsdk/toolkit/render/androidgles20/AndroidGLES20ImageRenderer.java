package vsdk.toolkit.render.androidgles20;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;

public class AndroidGLES20ImageRenderer extends AndroidGLES20Renderer
{
    public static int activate(Image img)
    {
        if ( img == null ) {
            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES20ImageRenderer.activate",
            "Trying to activate a NULL Image!");
        }

        if ( img instanceof RGBAImageUncompressed ) {
            return AndroidGLES20RGBAImageUncompressedRenderer.activate((RGBAImageUncompressed)img);
        }
        else if ( img instanceof RGBImageUncompressed ) {
            return AndroidGLES20RGBImageUncompressedRenderer.activate((RGBImageUncompressed)img);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES20ImageRenderer.activate",
            "Image GL activation not implemented for subclass " + c);
        }
        return -1;
    }

}
