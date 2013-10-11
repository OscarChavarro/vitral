//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

public class JoglStereoStrategyMultiviewRenderer extends JoglStereoStrategyRenderer
{
    private int viewport[];

    public JoglStereoStrategyMultiviewRenderer()
    {
        super();
        viewport = new int[4];
    }

    @Override
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

    @Override
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

    @Override
    public void activateStereoMode(GL2 gl)
    {
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
    }

    @Override
    public void deactivateStereoMode(GL2 gl)
    {
        gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
