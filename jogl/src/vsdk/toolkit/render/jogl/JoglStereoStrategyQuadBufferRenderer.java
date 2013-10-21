//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilities;

public class JoglStereoStrategyQuadBufferRenderer extends JoglStereoStrategyRenderer
{
    @Override
    public void requestCapabilities(GLCapabilities caps)
    {
        caps.setStereo(true);
    }

    public JoglStereoStrategyQuadBufferRenderer()
    {
        super();
    }

    @Override
    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        if ( swapChannels == false ) {
            gl.glDrawBuffer(GL2GL3.GL_BACK_LEFT);
        }
        else {
            gl.glDrawBuffer(GL2GL3.GL_BACK_RIGHT);
        }
        return true;
    }

    @Override
    public boolean configureDefaultRightChannel(GL2 gl)
    {
        if ( swapChannels == false ) {
            gl.glDrawBuffer(GL2GL3.GL_BACK_RIGHT);
        }
        else {
            gl.glDrawBuffer(GL2GL3.GL_BACK_LEFT);
        }
        return true;
    }

    @Override
    public void activateStereoMode(GL2 gl)
    {
    }

    @Override
    public void deactivateStereoMode(GL2 gl)
    {
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
