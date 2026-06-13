package render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Locale;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;

import models.GizmoNames;
import models.TangibleInterfaceGizmosModel;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;
import vsdk.toolkit.render.jogl.polyhedralBoundedSolid.Jogl4PolyhedralBoundedSolidDebugHUDRenderer;

public class Jogl4DebuggerHudRenderer
{
    private static final int LINE_HEIGHT = 34;
    private static final int HUD_TOP_PADDING = 28;
    private static final int HUD_LEFT_PADDING = 16;
    private static final int HUD_BOTTOM_PADDING = 16;

    private final TangibleInterfaceGizmosModel model;
    private final Font hudFont;
    private final Font labelFont;
    private RGBAImageUncompressed overlayImage;
    private BufferedImage bufferedOverlay;
    private int viewportWidth;
    private int viewportHeight;

    public Jogl4DebuggerHudRenderer(TangibleInterfaceGizmosModel model)
    {
        this.model = model;
        this.hudFont = new Font("SansSerif", Font.BOLD, 18);
        this.labelFont = new Font("SansSerif", Font.PLAIN, 12);
        this.overlayImage = null;
        this.bufferedOverlay = null;
        this.viewportWidth = 0;
        this.viewportHeight = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        updateViewportSize(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    }

    public void updateViewportSize(int width, int height)
    {
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(1, height);
    }

    public void draw(GLAutoDrawable drawable)
    {
        if ( drawable == null || model == null ) {
            return;
        }

        GL4 gl = drawable.getGL().getGL4();
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        int width = viewportWidth > 0 ? viewportWidth : Math.max(1, viewport[2]);
        int height = viewportHeight > 0 ? viewportHeight : Math.max(1, viewport[3]);
        updateViewportSize(width, height);
        ensureOverlayBuffers(width, height);

        Graphics2D g = bufferedOverlay.createGraphics();
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, width, height);

        drawHudText(g, width, height);
        drawSelectedFaceLabel(g);
        drawDebugVertexLabels(g);
        g.dispose();

        copyBufferedOverlayToImage(width, height);
        Jogl4ImageRenderer.unload(gl, overlayImage);
        Jogl4ImageRenderer.draw(gl, overlayImage);
    }

    public void dispose(GLAutoDrawable drawable)
    {
        if ( drawable != null && overlayImage != null ) {
            Jogl4ImageRenderer.unload(drawable.getGL().getGL4(), overlayImage);
        }
        overlayImage = null;
        bufferedOverlay = null;
    }

    private void drawHudText(Graphics2D g, int width, int height)
    {
        int blockHeight = HUD_TOP_PADDING + 5 * LINE_HEIGHT + HUD_BOTTOM_PADDING;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, width, Math.min(height, blockHeight));
        g.setFont(hudFont);
        g.setColor(new Color(255, 242, 51));

        String showingFaceLoopMessage = "Face [1, 2]: " + formatFaceLoopLabel();
        String selectedModelMessage = "Selected model [3, 4]: "
            + model.getSolidModelName().name()
            + " (" + model.getSolidModelName().getDisplayIndex()
            + "/" + GizmoNames.getTotalModels() + ")";
        String verticesMessage = "Vertices [v]: "
            + (model.notDebugVertices() ? "OFF" : "ON");
        String referenceFrameMessage = "Reference frame [Space]: "
            + (model.isShowCoordinateSystem() ? "ON" : "OFF");
        String geometryControlsMessage = String.format(Locale.US,
            "Inner radius (5/6): %.2f, outterRadius (7/8): %.2f, base height (9/0): %.2f",
            model.getInnerRadius(), model.getOuterRadius(), model.getBaseHeight());

        g.drawString(showingFaceLoopMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING);
        g.drawString(selectedModelMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING + LINE_HEIGHT);
        g.drawString(verticesMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING + 2 * LINE_HEIGHT);
        g.drawString(referenceFrameMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING + 3 * LINE_HEIGHT);
        g.drawString(geometryControlsMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING + 4 * LINE_HEIGHT);

        if ( model.isErrorState() ) {
            g.setColor(new Color(255, 38, 38));
            g.drawString(model.getErrorMessage(), HUD_LEFT_PADDING, height - 16);
        }
    }

    private void drawDebugVertexLabels(Graphics2D g)
    {
        PolyhedralBoundedSolid solid = model.getSolid();
        if ( model.notDebugVertices() || solid == null || solid.getVerticesList() == null ) {
            return;
        }
        g.setFont(labelFont);
        Jogl4PolyhedralBoundedSolidDebugHUDRenderer.drawDebugVertexLabels(g,
            solid, model.getCamera(), viewportWidth, viewportHeight);
    }

    private void drawSelectedFaceLabel(Graphics2D g)
    {
        g.setFont(labelFont);
        Jogl4PolyhedralBoundedSolidDebugHUDRenderer.drawSelectedFaceLabel(g,
            model.getSolid(), model.getFaceIndex(), model.getCamera(),
            viewportWidth, viewportHeight);
    }

    private String formatFaceLoopLabel()
    {
        if ( model.getFaceIndex() == -2 ) {
            return "NONE";
        }
        if ( model.getFaceIndex() == -1 ) {
            return "ALL";
        }

        int currentFace = model.getFaceIndex() + 1;
        int totalFaces = 0;
        if ( model.getSolid() != null && model.getSolid().getPolygonsList() != null ) {
            totalFaces = model.getSolid().getPolygonsList().size();
        }
        return "[" + currentFace + "/" + totalFaces + "]";
    }

    private void ensureOverlayBuffers(int width, int height)
    {
        if ( overlayImage != null && bufferedOverlay != null &&
             overlayImage.getXSize() == width && overlayImage.getYSize() == height ) {
            return;
        }
        overlayImage = new RGBAImageUncompressed();
        overlayImage.init(width, height);
        bufferedOverlay = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private void copyBufferedOverlayToImage(int width, int height)
    {
        for ( int y = 0; y < height; y++ ) {
            for ( int x = 0; x < width; x++ ) {
                int rgba = bufferedOverlay.getRGB(x, y);
                byte r = (byte)((rgba >> 16) & 0xFF);
                byte g = (byte)((rgba >> 8) & 0xFF);
                byte b = (byte)(rgba & 0xFF);
                byte a = (byte)((rgba >> 24) & 0xFF);
                overlayImage.putPixel(x, y, r, g, b, a);
            }
        }
    }
}
