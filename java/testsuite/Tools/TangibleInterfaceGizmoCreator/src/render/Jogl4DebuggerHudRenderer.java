package render;

import java.awt.Font;
import java.awt.geom.Rectangle2D;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;

import models.GizmoNames;
import models.TangibleInterfaceGizmosModel;

public class Jogl4DebuggerHudRenderer
{
    private static final int LINE_HEIGHT = 34;

    private final TangibleInterfaceGizmosModel model;
    private TextRenderer hudTextRenderer;
    private int viewportWidth;
    private int viewportHeight;

    public Jogl4DebuggerHudRenderer(TangibleInterfaceGizmosModel model)
    {
        this.model = model;
        this.hudTextRenderer = null;
        this.viewportWidth = 0;
        this.viewportHeight = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        hudTextRenderer = new TextRenderer(
            new Font("SansSerif", Font.BOLD, 18), true, true);
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
        String selectedModelMessage = "Selected model [3, 4]: "
            + model.getSolidModelName().name()
            + " (" + model.getSolidModelName().getDisplayIndex()
            + "/" + GizmoNames.getTotalModels() + ")";
        String referenceFrameMessage = "Reference frame [Space]: "
            + (model.isShowCoordinateSystem() ? "ON" : "OFF");
        String nrMessage = "NR [q, Q]: " + model.getSubdivisionCircumference();
        String nhMessage = "NH [w, W]: " + model.getSubdivisionHeight();

        hudTextRenderer.beginRendering(width, height);
        hudTextRenderer.setColor(1.0f, 1.0f, 0.0f, 1.0f);
        hudTextRenderer.draw(selectedModelMessage, 16, height - 28);
        hudTextRenderer.draw(referenceFrameMessage, 16,
            height - (28 + LINE_HEIGHT));
        drawTopRight(hudTextRenderer, width, height, nrMessage, 28);
        drawTopRight(hudTextRenderer, width, height, nhMessage,
            28 + LINE_HEIGHT);
        if ( model.isErrorState() ) {
            hudTextRenderer.setColor(1.0f, 0.1f, 0.1f, 1.0f);
            hudTextRenderer.draw(model.getErrorMessage(), 16, 16);
        }
        hudTextRenderer.endRendering();
    }

    public void dispose(GLAutoDrawable drawable)
    {
        if ( hudTextRenderer != null ) {
            hudTextRenderer.dispose();
            hudTextRenderer = null;
        }
    }

    private static void drawTopRight(TextRenderer renderer, int width,
        int height, String text, int offsetFromTop)
    {
        Rectangle2D textBounds = renderer.getBounds(text);
        int x = width - 16 - (int)Math.ceil(textBounds.getWidth());
        int y = height - (int)Math.round((double)offsetFromTop);
        renderer.draw(text, x, y);
    }
}
