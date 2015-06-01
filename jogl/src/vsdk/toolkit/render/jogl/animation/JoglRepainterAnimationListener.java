//===========================================================================
package vsdk.toolkit.render.jogl.animation;

// JOGL classes
import com.jogamp.opengl.awt.GLJPanel;

// VSDK classes
import vsdk.toolkit.animation.AnimationEvent;
import vsdk.toolkit.animation.AnimationListener;

/**
This listener, which is dependent on JOGL, has the intent of querying a
canvas update over an associated GLJPanel as a response of an animation
event. It is to be used in conjunction with other animation listeners
in order to constitute an animation.
*/
public class JoglRepainterAnimationListener extends AnimationListener {
    private final GLJPanel canvas;
    
    private boolean paused;
    private int pauseCounter;

    public JoglRepainterAnimationListener(GLJPanel canvas)
    {
        this.canvas = canvas;
        paused = false;
    }

    @Override
    public void tick(AnimationEvent e) {
        
        if ( isPaused() ) {
            pauseCounter++;
        }
        
        if ( canvas != null && 
             (!isPaused() || (isPaused() && pauseCounter > 20) ) ) {
            pauseCounter = 0;
            canvas.repaint();
        }
    }

    /**
     * @return the paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * @param paused the paused to set
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
