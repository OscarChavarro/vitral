//=   getImageJOGL methods added                                            =

package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBAImageCompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;

public class Jogl2ImageRenderer extends Jogl2Renderer
{
    /**
    This method generates an OpenGL/JOGL MipMap structure, associates it with
    the given image reference and activates.

    The method keeps track of all images activated, and take that history into
    account to pass the image data to the graphics hardware only once. Note that
    this method creates and use an OpenGL/JOGL compilation list for each image,
    to ensure optimal performance.

    \todo
    In applications with changing images, the memory list of compiled lists
    and the list themselves should be cleared, or not used. This will lead to
    the creation of new methods.
    @param gl
    @param img
    @return 
    */
    public static int activate(GL2 gl, Image img)
    {
        if ( img == null ) {
            return -1;
        }
        if ( img instanceof RGBAImageCompressed ) {
            return Jogl2RGBAImageCompressedRenderer.activate(gl, (RGBAImageCompressed)img);
        }
        else if ( img instanceof RGBAImageUncompressed ) {
            return Jogl2RGBAImageUncompressedRenderer.activate(gl, (RGBAImageUncompressed)img);
        }
        else if ( img instanceof RGBImageUncompressed ) {
            return Jogl2RGBImageUncompressedRenderer.activate(gl, (RGBImageUncompressed)img);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "Jogl2ImageRenderer.activate",
            "Image GL activation not implemented for subclass " + c);
        }
        return -1;
    }

    public static void deactivate(GL2 gl, Image img)
    {
        if ( img instanceof RGBAImageCompressed ) {
            Jogl2RGBAImageCompressedRenderer.deactivate(gl, (RGBAImageCompressed)img);
        }
        else if ( img instanceof RGBImageUncompressed ) {
            Jogl2RGBImageUncompressedRenderer.deactivate(gl, ((RGBImageUncompressed)img));
        }
    }

    public static int activateAsNormalMap(GL2 gl, Image img, RendererConfiguration quality)
    {
        if ( img instanceof RGBAImageUncompressed ) {
            return Jogl2RGBAImageUncompressedRenderer.activateAsNormalMap(gl, (RGBAImageUncompressed)img, quality);
        }
        else if ( img instanceof RGBImageUncompressed ) {
            return Jogl2RGBImageUncompressedRenderer.activateAsNormalMap(gl, (RGBImageUncompressed)img, quality);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "Jogl2ImageRenderer.activateAsNormalMap",
            "Image GL activation not implemented for subclass " + c);
        }
        return -1;
    }

    public static void unload(GL2 gl, Image img)
    {
        if ( img instanceof RGBAImageCompressed ) {
            Jogl2RGBAImageCompressedRenderer.unload(gl, (RGBAImageCompressed)img);
        }
        else if ( img instanceof RGBImageUncompressed ) {
            Jogl2RGBImageUncompressedRenderer.unload(gl, (RGBImageUncompressed)img);
        }
        else if ( img instanceof RGBAImageUncompressed ) {
            Jogl2RGBAImageUncompressedRenderer.unload(gl, (RGBAImageUncompressed)img);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "Jogl2ImageRenderer.unload",
            "Image GL unloading not implemented for subclass " + c);
        }
    }

    public static void draw(GL2 gl, Image img)
    {
        if ( img instanceof RGBAImageCompressed ) {
            Jogl2RGBAImageCompressedRenderer.draw(gl, (RGBAImageCompressed)img);
        }
        else if ( img instanceof RGBAImageUncompressed ) {
            Jogl2RGBAImageUncompressedRenderer.draw(gl, (RGBAImageUncompressed)img);
        }
        else if ( img instanceof RGBImageUncompressed ) {
            Jogl2RGBImageUncompressedRenderer.draw(gl, (RGBImageUncompressed)img);
        }
        else {
            String c = img.getClass().getName();

            VSDK.reportMessage(null, VSDK.WARNING, "Jogl2ImageRenderer.draw",
            "Image GL drawing not implemented for subclass " + c);
        }
    }

}
