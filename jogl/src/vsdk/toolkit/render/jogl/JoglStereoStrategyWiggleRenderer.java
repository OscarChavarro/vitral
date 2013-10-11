//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL2;

import vsdk.toolkit.common.StopWatch;

public class JoglStereoStrategyWiggleRenderer extends JoglStereoStrategyRenderer
{
    private StopWatch clock;
    private int channelToShow;
    private boolean inFrame;
    private double timeLimit;

    public JoglStereoStrategyWiggleRenderer(double timeLimit)
    {
        swapChannels = false;
        clock = new StopWatch();
        clock.start();
        channelToShow = 1;
        inFrame = false;
        this.timeLimit = timeLimit;
    }

    private void checkTime()
    {
        clock.stop();
        if ( clock.getElapsedRealTime() > timeLimit ) {
            clock.start();
            if ( channelToShow == 1 ) {
                channelToShow = 2;
            }
            else {
                channelToShow = 1;
            }
        }
    }


    @Override
    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        if ( !inFrame || channelToShow != 1 ) {
            return false;
        }
        else {
            inFrame = false;
            checkTime();
            return true;
        }
    }

    @Override
    public boolean configureDefaultRightChannel(GL2 gl)
    {
        if ( !inFrame || channelToShow != 2 ) {
            return false;
        }
        else {
            inFrame = false;
            checkTime();
            return true;
        }
    }

    @Override
    public void activateStereoMode(GL2 gl)
    {
        inFrame = true;
    }

    @Override
    public void deactivateStereoMode(GL2 gl)
    {
        inFrame = false;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
