package animation;

import com.jogamp.opengl.awt.GLJPanel;

import vsdk.toolkit.animation.AnimationEventGenerator;
import vsdk.toolkit.animation.Md2AnimationListener;
import vsdk.toolkit.environment.geometry.surface.Md2Mesh;
import vsdk.toolkit.render.jogl.animation.JoglRepainterAnimationListener;

public class DebuggerAnimationController {
    private AnimationEventGenerator animator;

    public void start(Md2Mesh md2Mesh, GLJPanel panel) {
        animator = new AnimationEventGenerator();
        Md2AnimationListener md2AniListener = new Md2AnimationListener(md2Mesh);
        animator.addAnimationListener(md2AniListener);

        JoglRepainterAnimationListener repainterListener;
        repainterListener = new JoglRepainterAnimationListener(panel);
        animator.addAnimationListener(repainterListener);

        Thread t = new Thread(animator);
        t.start();
    }
}
