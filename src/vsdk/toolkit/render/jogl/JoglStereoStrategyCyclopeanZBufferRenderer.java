//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;

import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.render.jogl.JoglZBufferRenderer;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.media.RGBColorPalette;

public class JoglStereoStrategyCyclopeanZBufferRenderer extends JoglStereoStrategyRenderer
{
    private RGBColorPalette palette = null;

    public JoglStereoStrategyCyclopeanZBufferRenderer()
    {
        super();
    }

    public JoglStereoStrategyCyclopeanZBufferRenderer(RGBColorPalette palette)
    {
        super();
        this.palette = palette;
    }

    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        return false;
    }

    public boolean configureDefaultRightChannel(GL2 gl)
    {
        return true;
    }

    public void activateStereoMode(GL2 gl)
    {
        ;
    }

    public void deactivateStereoMode(GL2 gl)
    {
        ZBuffer zbuffer;
        RGBImage image;

        zbuffer = JoglZBufferRenderer.importJOGLZBuffer(gl);

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

        JoglImageRenderer.draw(gl, image);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
