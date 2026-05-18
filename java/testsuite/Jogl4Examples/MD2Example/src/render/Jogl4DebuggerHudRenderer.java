package render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;

import model.DebuggerModel;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;

public class Jogl4DebuggerHudRenderer {
    private static final int HUD_HEIGHT = 64;
    private static final int HUD_LEFT = 16;
    private static final int HUD_BASELINE = 42;

    private final DebuggerModel model;
    private final Font hudFont;
    private RGBImageUncompressed hudImage;
    private BufferedImage bufferedHud;
    private int hudWidth;
    private int hudHeight;

    public Jogl4DebuggerHudRenderer(DebuggerModel model)
    {
        this.model = model;
        this.hudFont = new Font("SansSerif", Font.BOLD, 20);
        this.hudWidth = 0;
        this.hudHeight = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        // HUD buffers are lazily created with current viewport dimensions.
    }

    public void updateViewportSize(int width, int height)
    {
        // HUD buffers are validated on each draw.
    }

    public void draw(GLAutoDrawable drawable)
    {
        if ( drawable == null || model == null ) {
            return;
        }

        GL4 gl = drawable.getGL().getGL4();
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        int targetWidth = Math.max(1, viewport[2]);
        int targetHeight = Math.min(HUD_HEIGHT, Math.max(1, viewport[3]));
        ensureHudBuffers(targetWidth, targetHeight);

        int totalAnimations = getTotalAnimations();
        int selectedAnimation = Math.min(model.md2Mesh.getCurrentAnimationInd() + 1, totalAnimations);
        String animationName = model.md2Mesh.getAnimationName(model.md2Mesh.getCurrentAnimationInd());
        String msg = "Selected animation [1, 2]: " + selectedAnimation + "/" + totalAnimations
            + " " + animationName.toUpperCase();
        String selectedObjectMsg = "Selected object to move with XYZ [3, 4]: "
            + getSelectedObjectName();

        Graphics2D g = bufferedHud.createGraphics();
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, hudWidth, hudHeight);
        g.setColor(Color.WHITE);
        g.setFont(hudFont);
        g.drawString(msg, HUD_LEFT, HUD_BASELINE);
        int rightX = Math.max(HUD_LEFT, hudWidth - HUD_LEFT -
            g.getFontMetrics().stringWidth(selectedObjectMsg));
        g.drawString(selectedObjectMsg, rightX, HUD_BASELINE);
        g.dispose();

        for ( int y = 0; y < hudHeight; y++ ) {
            for ( int x = 0; x < hudWidth; x++ ) {
                int rgb = bufferedHud.getRGB(x, y);
                byte r = (byte)((rgb >> 16) & 0xFF);
                byte gr = (byte)((rgb >> 8) & 0xFF);
                byte b = (byte)(rgb & 0xFF);
                hudImage.putPixel(x, y, r, gr, b);
            }
        }

        Jogl4ImageRenderer.unload(gl, hudImage);

        int hudX = viewport[0];
        int hudY = viewport[1] + viewport[3] - hudHeight;
        gl.glViewport(hudX, hudY, hudWidth, hudHeight);
        Jogl4ImageRenderer.draw(gl, hudImage);
        gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    public void dispose(GLAutoDrawable drawable)
    {
        // No-op. OpenGL texture is recreated on each draw via unload+draw.
    }

    private int getTotalAnimations()
    {
        short[] animStartEnd = new short[2];
        model.md2Mesh.returnStartEndAnim(model.md2Mesh.getCurrentAnimationInd(), animStartEnd);
        return Math.max(1, model.md2Mesh.getMaxAnimationInd() + 1);
    }

    private void ensureHudBuffers(int width, int height)
    {
        if ( hudImage != null && bufferedHud != null &&
             hudWidth == width && hudHeight == height ) {
            return;
        }
        hudWidth = width;
        hudHeight = height;
        hudImage = new RGBImageUncompressed();
        hudImage.init(hudWidth, hudHeight);
        bufferedHud = new BufferedImage(
            hudWidth,
            hudHeight,
            BufferedImage.TYPE_INT_RGB);
    }

    private String getSelectedObjectName()
    {
        if ( model.selectedObject < 0 ) {
            return "Camera";
        }
        return "Light " + (model.selectedObject + 1);
    }
}
