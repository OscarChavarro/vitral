package animation;

import com.jogamp.opengl.awt.GLCanvas;

import model.MeshModel;
import vsdk.toolkit.animation.AnimationEvent;
import vsdk.toolkit.animation.AnimationEventGenerator;
import vsdk.toolkit.animation.AnimationListener;

public class AnimationController {
    private AnimationEventGenerator animator;
    private boolean started;

    public void start(MeshModel model, GLCanvas panel) {
        if ( started ) {
            return;
        }

        animator = new AnimationEventGenerator();
        animator.addAnimationListener(new AnimationListener() {
            private double lastTickT = 0.0;

            @Override
            public void tick(AnimationEvent e) {
                if ( e.getT() - lastTickT >= 1.0 ) {
                    model.getRayGizmo().update();
                    panel.repaint();
                    lastTickT = e.getT();
                }
            }
        });

        Thread t = new Thread(animator);
        t.setDaemon(true);
        t.start();
        started = true;
    }
}
