//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.GLCapabilities;

public class JoglStereoStrategyMultiviewRenderer extends JoglStereoStrategyRenderer
{
    private int viewport[];

    public JoglStereoStrategyMultiviewRenderer()
    {
        super();
        viewport = new int[4];
    }

    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        if ( swapChannels  ) {
            gl.glViewport(viewport[0] + viewport[2]/2,
                          viewport[1], viewport[2]/2, viewport[3]);
        }
        else {
            gl.glViewport(viewport[0], viewport[1], viewport[2]/2, viewport[3]);
        }
        return true;
    }

    public boolean configureDefaultRightChannel(GL2 gl)
    {
        if ( swapChannels  ) {
            gl.glViewport(viewport[0], viewport[1], viewport[2]/2, viewport[3]);
        }
        else {
            gl.glViewport(viewport[0] + viewport[2]/2,
                          viewport[1], viewport[2]/2, viewport[3]);
        }
        return true;
    }

    public void activateStereoMode(GL2 gl)
    {
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
    }

    public void deactivateStereoMode(GL2 gl)
    {
        gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
