//===========================================================================
package vsdk.toolkit.render.jogl.animation;

// JOGL classes
import javax.media.opengl.awt.GLJPanel;

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

    public JoglRepainterAnimationListener(GLJPanel canvas)
    {
        this.canvas = canvas;
    }

    @Override
    public void tick(AnimationEvent e) {
        if ( canvas != null ) {
            canvas.repaint();
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
