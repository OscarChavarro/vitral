package animation;

import com.jogamp.opengl.awt.GLCanvas;

import models.DebuggerModel;
import vsdk.toolkit.animation.AnimationEvent;
import vsdk.toolkit.animation.AnimationEventGenerator;
import vsdk.toolkit.animation.AnimationListener;

public class SolidAnimationController
{
    private static final double ROTATION_STEP_DEGREES = 1.0;

    private AnimationEventGenerator animator;
    private boolean started;

    public void start(DebuggerModel model, GLCanvas panel)
    {
        if ( started ) {
            return;
        }

        animator = new AnimationEventGenerator();
        animator.addAnimationListener(new AnimationListener() {
            @Override
            public void tick(AnimationEvent e)
            {
                if ( !model.isSolidAnimationEnabled() ) {
                    return;
                }
                model.rotateSolidAroundZDegrees(ROTATION_STEP_DEGREES);
                panel.repaint();
            }
        });

        Thread t = new Thread(animator);
        t.setDaemon(true);
        t.start();
        started = true;
    }

    public void toggleAnimation(DebuggerModel model)
    {
        model.toggleSolidAnimationEnabled();
    }
}
