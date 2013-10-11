//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;

public abstract class JoglStereoStrategyRenderer
{
    protected boolean swapChannels;

    public void requestCapabilities(GLCapabilities caps)
    {
        ;
    }

    public JoglStereoStrategyRenderer()
    {
        swapChannels = false;
    }

    public void setSwapChannels(boolean state)
    {
        swapChannels = state;
    }

    public boolean getSwapChannels()
    {
        return swapChannels;
    }

    public abstract boolean configureDefaultLeftChannel(GL2 gl);
    public abstract boolean configureDefaultRightChannel(GL2 gl);

    public abstract void activateStereoMode(GL2 gl);
    public abstract void deactivateStereoMode(GL2 gl);

    public boolean configureLeftChannel(GL2 gl)
    {
        if ( !swapChannels ) {
            return configureDefaultLeftChannel(gl);
        }
        return configureDefaultRightChannel(gl);
    }

    public boolean configureRightChannel(GL2 gl)
    {
        if ( !swapChannels ) {
            return configureDefaultRightChannel(gl);
        }
        return configureDefaultLeftChannel(gl);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
