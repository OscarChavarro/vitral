package animation;

import com.jogamp.opengl.awt.GLCanvas;

import vsdk.toolkit.animation.AnimationEvent;
import vsdk.toolkit.animation.AnimationEventGenerator;
import vsdk.toolkit.animation.AnimationListener;
import vsdk.toolkit.animation.Md2AnimationListener;
import vsdk.toolkit.environment.geometry.surface.Md2Mesh;

public class DebuggerAnimationController {
    private AnimationEventGenerator animator;

    public void start(Md2Mesh md2Mesh, GLCanvas panel) {
        animator = new AnimationEventGenerator();
        Md2AnimationListener md2AniListener = new Md2AnimationListener(md2Mesh);
        animator.addAnimationListener(md2AniListener);

        AnimationListener repainterListener = new AnimationListener() {
            @Override
            public void tick(AnimationEvent e)
            {
                panel.repaint();
            }
        };
        animator.addAnimationListener(repainterListener);

        Thread t = new Thread(animator);
        t.start();
    }
}
