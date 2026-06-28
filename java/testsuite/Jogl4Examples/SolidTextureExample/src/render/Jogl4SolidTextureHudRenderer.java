package render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL4;

import model.SolidTextureModel;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;

public class Jogl4SolidTextureHudRenderer {
    private static final int HUD_HEIGHT = 72;
    private static final int HUD_LEFT = 16;
    private static final int HUD_BASELINE_1 = 28;
    private static final int HUD_BASELINE_2 = 54;

    private final SolidTextureModel model;
    private final Font hudFont;
    private RGBImageUncompressed hudImage;
    private BufferedImage bufferedHud;
    private int hudWidth;
    private int hudHeight;

    public Jogl4SolidTextureHudRenderer(SolidTextureModel model)
    {
        this.model = model;
        this.hudFont = new Font("SansSerif", Font.BOLD, 18);
        this.hudWidth = 0;
        this.hudHeight = 0;
    }

    public void draw(GL4 gl)
    {
        if ( gl == null || model == null ) {
            return;
        }

        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        int targetWidth = Math.max(1, viewport[2]);
        int targetHeight = Math.min(HUD_HEIGHT, Math.max(1, viewport[3]));
        ensureHudBuffers(targetWidth, targetHeight);

        Graphics2D g = bufferedHud.createGraphics();
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, hudWidth, hudHeight);
        g.setColor(Color.WHITE);
        g.setFont(hudFont);
        g.drawString("Operation mode [1]: " + model.getOperationMode().name(),
            HUD_LEFT, HUD_BASELINE_1);
        g.drawString("Texture side [2, 3]: " + model.getSolidTextureSize(),
            HUD_LEFT, HUD_BASELINE_2);
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

        gl.glViewport(viewport[0], viewport[1] + viewport[3] - hudHeight,
            hudWidth, hudHeight);
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
}
