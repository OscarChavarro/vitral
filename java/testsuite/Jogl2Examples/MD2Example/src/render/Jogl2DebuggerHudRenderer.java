package render;

import java.awt.Font;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;

import model.DebuggerModel;

public class Jogl2DebuggerHudRenderer {
    private final DebuggerModel model;
    private TextRenderer hudTextRenderer;
    private int viewportWidth;
    private int viewportHeight;

    public Jogl2DebuggerHudRenderer(DebuggerModel model)
    {
        this.model = model;
        this.hudTextRenderer = null;
        this.viewportWidth = 0;
        this.viewportHeight = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        hudTextRenderer = new TextRenderer(
            new Font("SansSerif", Font.BOLD, 20), true, true);
        updateViewportSize(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    }

    public void updateViewportSize(int width, int height)
    {
        viewportWidth = width;
        viewportHeight = height;
    }

    public void draw(GLAutoDrawable drawable)
    {
        if ( hudTextRenderer == null ) {
            return;
        }

        int width = viewportWidth > 0 ? viewportWidth : drawable.getSurfaceWidth();
        int height = viewportHeight > 0 ? viewportHeight : drawable.getSurfaceHeight();
        int totalAnimations = getTotalAnimations();
        int selectedAnimation = Math.min(model.md2Mesh.getCurrentAnimationInd() + 1, totalAnimations);
        String animationName = model.md2Mesh.getAnimationName(model.md2Mesh.getCurrentAnimationInd());
        String msg = "Selected animation [1, 2]: " + selectedAnimation + "/" + totalAnimations
            + " " + animationName.toUpperCase();

        hudTextRenderer.beginRendering(width, height);
        hudTextRenderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        hudTextRenderer.draw(msg, 16, height - 40);
        hudTextRenderer.endRendering();
    }

    public void dispose(GLAutoDrawable drawable)
    {
        if ( hudTextRenderer != null ) {
            hudTextRenderer.dispose();
            hudTextRenderer = null;
        }
    }

    private int getTotalAnimations()
    {
        short[] animStartEnd = new short[2];
        model.md2Mesh.returnStartEndAnim(model.md2Mesh.getCurrentAnimationInd(), animStartEnd);
        return Math.max(1, model.md2Mesh.getMaxAnimationInd() + 1);
    }
}
