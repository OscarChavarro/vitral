import java.awt.Font;
import java.awt.geom.Rectangle2D;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;

public class JoglPolygonClippingHudRenderer
{
    private static final int LINE_HEIGHT = 28;

    private final PolygonClippingDebuggerModel model;
    private TextRenderer hudTextRenderer;
    private int viewportWidth;
    private int viewportHeight;

    public JoglPolygonClippingHudRenderer(PolygonClippingDebuggerModel model)
    {
        this.model = model;
        hudTextRenderer = null;
        viewportWidth = 0;
        viewportHeight = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        hudTextRenderer = new TextRenderer(
            new Font("SansSerif", Font.BOLD, 17), true, true);
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

        PolygonClippingTestCase testCase = model.getCurrentTestCase();
        int width = viewportWidth > 0 ? viewportWidth : drawable.getSurfaceWidth();
        int height = viewportHeight > 0 ? viewportHeight : drawable.getSurfaceHeight();

        String testMessage = "Test [1, 2]: " + testCase.name()
            + " (" + (model.testIndex + 1) + "/" + model.getTotalTestCases() + ")";
        String sourcesMessage = "Clip [C]: " + onOff(model.showClipPolygon)
            + "  Subject [S]: " + onOff(model.showSubjectPolygon)
            + "  Points [P]: " + onOff(model.showIntersections);
        String outputsMessage = "Inner [I]: " + onOff(model.showInnerPolygon)
            + "  Outer [O]: " + onOff(model.showOuterPolygon)
            + "  Fill [T]: " + onOff(model.showFilledPolygons);
        String referenceFrameMessage = "Reference frame [Space]: "
            + onOff(model.showReferenceFrame);
        String countsMessage = "Loops C/S/I/O: "
            + countLoops(model.clipPolygon) + "/"
            + countLoops(model.subjectPolygon) + "/"
            + countLoops(model.innerPolygon) + "/"
            + countLoops(model.outerPolygon);
        String intersectionsMessage = "Intersections: "
            + PolygonClippingModelingTools.countPairedVertices(model.subjectPolygonWA);
        String utilityMessage = "Fullscreen [F]  Snapshot [H]";

        hudTextRenderer.beginRendering(width, height);
        hudTextRenderer.setColor(1.0f, 0.95f, 0.2f, 1.0f);
        hudTextRenderer.draw(testMessage, 16, height - 28);
        hudTextRenderer.draw(sourcesMessage, 16, height - (28 + LINE_HEIGHT));
        hudTextRenderer.draw(outputsMessage, 16, height - (28 + 2 * LINE_HEIGHT));
        hudTextRenderer.draw(referenceFrameMessage, 16,
            height - (28 + 3 * LINE_HEIGHT));
        drawTopRight(hudTextRenderer, width, height, countsMessage, 28);
        drawTopRight(hudTextRenderer, width, height, intersectionsMessage,
            28 + LINE_HEIGHT);
        drawTopRight(hudTextRenderer, width, height, utilityMessage,
            28 + 2 * LINE_HEIGHT);
        if ( model.errorState ) {
            hudTextRenderer.setColor(1.0f, 0.15f, 0.15f, 1.0f);
            hudTextRenderer.draw(model.errorMessage, 16, 16);
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

    private static int countLoops(vsdk.toolkit.environment.geometry.Polygon2D polygon)
    {
        if ( polygon == null || polygon.loops == null ) {
            return 0;
        }
        return polygon.loops.size();
    }

    private static String onOff(boolean value)
    {
        return value ? "ON" : "OFF";
    }

    private static void drawTopRight(TextRenderer renderer, int width,
        int height, String text, int offsetFromTop)
    {
        Rectangle2D textBounds = renderer.getBounds(text);
        int x = width - 16 - (int)Math.ceil(textBounds.getWidth());
        int y = height - offsetFromTop;
        renderer.draw(text, x, y);
    }
}
