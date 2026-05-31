package render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL4;
import model.PolygonClippingDebuggerModel;
import model.PolygonClippingModelingTools;
import model.PolygonClippingOperation;
import model.PolygonClippingTestCase;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;

public class JoglPolygonClippingHudRenderer
{
    private static final int LINE_HEIGHT = 38;
    private static final int HUD_TOP_PADDING = 34;
    private static final int HUD_BOTTOM_PADDING = 20;

    private final PolygonClippingDebuggerModel model;
    private final Font hudFont;
    private RGBImageUncompressed hudImage;
    private BufferedImage bufferedHud;
    private int viewportWidth;
    private int viewportHeight;

    public JoglPolygonClippingHudRenderer(PolygonClippingDebuggerModel model)
    {
        this.model = model;
        this.hudFont = new Font("SansSerif", Font.BOLD, 23);
        this.hudImage = null;
        this.bufferedHud = null;
        this.viewportWidth = 0;
        this.viewportHeight = 0;
    }

    public void init(int width, int height)
    {
        updateViewportSize(width, height);
        ensureHudBuffers(Math.max(1, viewportWidth), Math.max(1, viewportHeight));
    }

    public void updateViewportSize(int width, int height)
    {
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(1, height);
    }

    public void draw(GL4 gl)
    {
        if ( gl == null ) {
            return;
        }

        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        int width = viewportWidth > 0 ? viewportWidth : viewport[2];
        int height = viewportHeight > 0 ? viewportHeight : viewport[3];
        int hudHeight = Math.min(
            Math.max(1, height),
            HUD_TOP_PADDING + (5 * LINE_HEIGHT) + HUD_BOTTOM_PADDING);
        ensureHudBuffers(Math.max(1, width), hudHeight);

        PolygonClippingTestCase testCase = model.getCurrentTestCase();

        String testMessage = "Test [1, 2]: " + testCase.name()
            + " (" + (model.getTestIndex() + 1) + "/" + model.getTotalTestCases() + ")";
        String operationMessage = "Operation [3]: "
            + model.getOperation().getDisplayName();
        String sourcesMessage = "Clip [C]: " + onOff(model.isShowClipPolygon())
            + "  Subject [S]: " + onOff(model.isShowSubjectPolygon())
            + "  Points [P]: " + onOff(model.isShowIntersections());
        String outputsMessage = buildOutputsMessage(model);
        String referenceFrameMessage = "Reference frame [Space]: "
            + onOff(model.isShowReferenceFrame());
        String countsMessage = "Loops C/S/I/O: "
            + countLoops(model.getClipPolygon()) + "/"
            + countLoops(model.getSubjectPolygon()) + "/"
            + countLoops(model.getInnerPolygon()) + "/"
            + countLoops(model.getOuterPolygon());
        String intersectionsMessage = "Intersections: "
            + PolygonClippingModelingTools.countPairedVertices(model.getSubjectPolygonWA());
        String utilityMessage = "Fullscreen [F]  Snapshot [H]  Quality [F1/F2/F3]";

        Graphics2D g = bufferedHud.createGraphics();
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, hudHeight);
        g.setColor(new Color(255, 242, 51));
        g.setFont(hudFont);

        g.drawString(testMessage, 16, HUD_TOP_PADDING);
        g.drawString(operationMessage, 16, HUD_TOP_PADDING + LINE_HEIGHT);
        g.drawString(sourcesMessage, 16, HUD_TOP_PADDING + 2 * LINE_HEIGHT);
        g.drawString(outputsMessage, 16, HUD_TOP_PADDING + 3 * LINE_HEIGHT);
        g.drawString(referenceFrameMessage, 16, HUD_TOP_PADDING + 4 * LINE_HEIGHT);

        drawTopRight(g, width, countsMessage, HUD_TOP_PADDING);
        drawTopRight(g, width, intersectionsMessage, HUD_TOP_PADDING + LINE_HEIGHT);
        drawTopRight(g, width, utilityMessage, HUD_TOP_PADDING + 2 * LINE_HEIGHT);

        if ( model.isErrorState() ) {
            g.setColor(new Color(255, 38, 38));
            g.drawString(model.getErrorMessage(), 16, hudHeight - 10);
        }
        g.dispose();

        for ( int y = 0; y < hudHeight; y++ ) {
            for ( int x = 0; x < width; x++ ) {
                int rgb = bufferedHud.getRGB(x, y);
                byte r = (byte)((rgb >> 16) & 0xFF);
                byte gr = (byte)((rgb >> 8) & 0xFF);
                byte b = (byte)(rgb & 0xFF);
                hudImage.putPixel(x, y, r, gr, b);
            }
        }

        Jogl4ImageRenderer.unload(gl, hudImage);
        gl.glViewport(viewport[0], viewport[1] + viewport[3] - hudHeight,
            width, hudHeight);
        Jogl4ImageRenderer.draw(gl, hudImage);
        gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    public void dispose(GL4 gl)
    {
        if ( gl != null && hudImage != null ) {
            Jogl4ImageRenderer.unload(gl, hudImage);
        }
        hudImage = null;
        bufferedHud = null;
    }

    private void ensureHudBuffers(int width, int height)
    {
        if ( hudImage != null && bufferedHud != null &&
             hudImage.getXSize() == width && hudImage.getYSize() == height ) {
            return;
        }
        hudImage = new RGBImageUncompressed();
        hudImage.init(width, height);
        bufferedHud = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    private static void drawTopRight(Graphics2D g, int width, String text,
        int baselineY)
    {
        int textWidth = g.getFontMetrics().stringWidth(text);
        int x = width - 16 - textWidth;
        g.drawString(text, Math.max(16, x), baselineY);
    }

    private static int countLoops(Polygon2D polygon)
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

    private static String buildOutputsMessage(PolygonClippingDebuggerModel model)
    {
        if ( model.getOperation() == PolygonClippingOperation.INTERSECTION ) {
            return "Inner [I]: " + onOff(model.isShowInnerPolygon())
                + "  Outer [O]: " + onOff(model.isShowOuterPolygon())
                + "  Fill [T]: " + onOff(model.isShowFilledPolygons());
        }
        return "Result [I]: " + onOff(model.isShowInnerPolygon())
            + "  Secondary [O]: " + onOff(model.isShowOuterPolygon())
            + "  Fill [T]: " + onOff(model.isShowFilledPolygons());
    }
}
