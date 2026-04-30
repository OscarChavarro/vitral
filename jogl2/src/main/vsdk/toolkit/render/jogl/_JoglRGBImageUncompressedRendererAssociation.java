package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.util.texture.Texture;

// VitralSDK classes
import vsdk.toolkit.media.RGBImageUncompressed;

public class _JoglRGBImageUncompressedRendererAssociation extends Jogl2Renderer
{
    public int glList;
    public Texture renderer;
    public RGBImageUncompressed image;
}
