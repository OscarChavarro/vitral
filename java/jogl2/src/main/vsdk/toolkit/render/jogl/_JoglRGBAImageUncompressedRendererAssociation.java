package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.util.texture.Texture;

// VitralSDK classes
import vsdk.toolkit.media.RGBAImageUncompressed;

public class _JoglRGBAImageUncompressedRendererAssociation extends Jogl2Renderer
{
    public int glList;
    public Texture renderer;
    public RGBAImageUncompressed image;
}
