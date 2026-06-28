package animation;

import com.jogamp.opengl.awt.GLCanvas;

import model.SolidTextureModel;
import vsdk.toolkit.animation.AnimationEvent;
import vsdk.toolkit.animation.AnimationEventGenerator;
import vsdk.toolkit.animation.AnimationListener;

public class AnimationController {
    private static final double FULL_ROTATION_RADIANS = 2.0 * Math.PI;
    private static final double ROTATION_PERIOD_SECONDS = 8.0;
    private static final double ANGULAR_SPEED_RAD_PER_SECOND =
        FULL_ROTATION_RADIANS / ROTATION_PERIOD_SECONDS;
    private static final double MAX_ELAPSED_SECONDS = 0.25;

    private AnimationEventGenerator animator;
    private boolean started;

    public void start(SolidTextureModel model, GLCanvas panel) {
        if ( started ) {
            return;
        }

        animator = new AnimationEventGenerator();
        animator.addAnimationListener(new AnimationListener() {
            private double lastTickT = 0.0;
            private double lastRayGizmoUpdateT = 0.0;

            @Override
            public void tick(AnimationEvent e) {
                double elapsedSeconds = e.getT() - lastTickT;
                lastTickT = e.getT();

                boolean rayGizmoUpdated = false;
                if ( e.getT() - lastRayGizmoUpdateT >= 1.0 ) {
                    model.getRayGizmo().update();
                    lastRayGizmoUpdateT = e.getT();
                    rayGizmoUpdated = true;
                }

                if ( model.isAnimationEnabled() ) {
                    if ( elapsedSeconds < 0.0 ) {
                        elapsedSeconds = 0.0;
                    }
                    if ( elapsedSeconds > MAX_ELAPSED_SECONDS ) {
                        elapsedSeconds = MAX_ELAPSED_SECONDS;
                    }
                    model.advanceObjectRotationRadians(
                        ANGULAR_SPEED_RAD_PER_SECOND * elapsedSeconds);
                    panel.repaint();
                }
                else if ( rayGizmoUpdated ) {
                    panel.repaint();
                }
            }
        });

        Thread t = new Thread(animator);
        t.setDaemon(true);
        t.start();
        started = true;
    }
}
