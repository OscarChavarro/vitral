package vsdk.toolkit.render.androidgles10;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;

public class AndroidGLES10ImageRenderer extends AndroidGLES10Renderer
{
    public static int activate(Image img)
    {
        if ( img == null ) {
            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES10ImageRenderer.activate",
            "Trying to activate a NULL Image!");
        }

        if ( img instanceof RGBAImageUncompressed ) {
            return AndroidGLES10RGBAImageUncompressedRenderer.activate((RGBAImageUncompressed)img);
        }
        else if ( img instanceof RGBImageUncompressed ) {
            return AndroidGLES10RGBImageUncompressedRenderer.activate((RGBImageUncompressed)img);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES20ImageRenderer.activate",
            "Image GL activation not implemented for subclass " + c);
        }
        return -1;
    }
    
    public static void disable(Image img)
    {
        if ( img == null ) {
            return;
        }

        if ( img instanceof RGBAImageUncompressed ) {
            AndroidGLES10RGBAImageUncompressedRenderer.disable((RGBAImageUncompressed)img);
        }
        else if ( img instanceof RGBImageUncompressed ) {
            AndroidGLES10RGBImageUncompressedRenderer.disable((RGBImageUncompressed)img);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "AndroidGLES20ImageRenderer.disable",
            "Image GL activation not implemented for subclass " + c);
        }        
    }

}
