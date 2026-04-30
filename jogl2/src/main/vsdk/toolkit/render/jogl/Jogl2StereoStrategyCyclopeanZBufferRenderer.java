package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBColorPalette;

public class Jogl2StereoStrategyCyclopeanZBufferRenderer extends Jogl2StereoStrategyRenderer
{
    private RGBColorPalette palette = null;

    public Jogl2StereoStrategyCyclopeanZBufferRenderer()
    {
        super();
    }

    public Jogl2StereoStrategyCyclopeanZBufferRenderer(RGBColorPalette palette)
    {
        super();
        this.palette = palette;
    }

    @Override
    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        return false;
    }

    @Override
    public boolean configureDefaultRightChannel(GL2 gl)
    {
        return true;
    }

    @Override
    public void activateStereoMode(GL2 gl)
    {
    }

    @Override
    public void deactivateStereoMode(GL2 gl)
    {
        ZBuffer zbuffer;
        RGBImageUncompressed image;

        zbuffer = Jogl2ZBufferRenderer.importJOGLZBuffer(gl);

        if ( palette == null ) {
             palette = new RGBColorPalette();
        }

        image = zbuffer.exportRGBImage(palette);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

        Jogl2ImageRenderer.draw(gl, image);
    }
}
