package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLCapabilities;

public class Jogl2StereoStrategyQuadBufferRenderer extends Jogl2StereoStrategyRenderer
{
    @Override
    public void requestCapabilities(GLCapabilities caps)
    {
        caps.setStereo(true);
    }

    public Jogl2StereoStrategyQuadBufferRenderer()
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
