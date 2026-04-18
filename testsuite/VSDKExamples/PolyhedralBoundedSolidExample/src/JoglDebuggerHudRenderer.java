// Java Awt classes
import java.awt.Font;
import java.awt.geom.Rectangle2D;

// JOGL classes
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;

public class JoglDebuggerHudRenderer
{
    private static final int LINE_HEIGHT = 34;

    private final DebuggerModel model;
    private TextRenderer hudTextRenderer;
    private int viewportWidth;
    private int viewportHeight;

    public JoglDebuggerHudRenderer(DebuggerModel model)
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
        String showingFaceLoopMessage = "Face [1, 2]: " + formatFaceLoopLabel();
        String selectedModelMessage = "Selected model [3, 4]: "
            + model.solidModelName.name()
            + " (" + model.solidModelName.getDisplayIndex()
            + "/" + SolidModelNames.getTotalModels() + ")";
        String csgSampleMessage = "CSG sample [6]: " + model.csgSample.getLabel()
            + " (" + model.csgSample.getDisplayIndex()
            + "/" + CsgSampleNames.getTotalSamples() + ")";
        String csgOperationMessage = "CSG op [5]: " + model.csgOperation.getLabel();
        String referenceFrameMessage = "Reference frame [Space]: " +
            (model.showCoordinateSystem ? "ON" : "OFF");
        String nrMessage = "NR [q, Q]: " + model.subdivisionCircumference;
        String nhMessage = "NH [w, W]: " + model.subdivisionHeight;

        hudTextRenderer.beginRendering(width, height);
        hudTextRenderer.setColor(1.0f, 1.0f, 0.0f, 1.0f);
        hudTextRenderer.draw(showingFaceLoopMessage, 16, height - 28);
        hudTextRenderer.draw(selectedModelMessage, 16, height - (28 + LINE_HEIGHT));
        int nextLeftLine = 2;
        if ( model.solidModelName.usesCsgDebugControls() ) {
            hudTextRenderer.draw(csgSampleMessage, 16,
                height - (28 + nextLeftLine*LINE_HEIGHT));
            nextLeftLine++;
            hudTextRenderer.draw(csgOperationMessage, 16,
                height - (28 + nextLeftLine*LINE_HEIGHT));
            nextLeftLine++;
        }
        hudTextRenderer.draw(referenceFrameMessage, 16,
            height - (28 + nextLeftLine*LINE_HEIGHT));
        drawTopRight(hudTextRenderer, width, height, nrMessage, 28);
        drawTopRight(hudTextRenderer, width, height, nhMessage,
            28 + LINE_HEIGHT);
        if ( model.errorState ) {
            hudTextRenderer.setColor(1.0f, 0.1f, 0.1f, 1.0f);
            hudTextRenderer.draw(model.errorMessage, 16, 16);
        }
        hudTextRenderer.endRendering();
    }

    private String formatFaceLoopLabel()
    {
        if ( model.faceIndex == -2 ) {
            return "NONE";
        }
        if ( model.faceIndex == -1 ) {
            return "ALL";
        }

        int currentFace = model.faceIndex + 1;
        int totalFaces = 0;
        if ( model.solid != null && model.solid.polygonsList != null ) {
            totalFaces = model.solid.polygonsList.size();
        }
        return "[" + currentFace + "/" + totalFaces + "]";
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
