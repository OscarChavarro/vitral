package render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL4;

import model.ShaderOperationMode;
import model.ShadersModel;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;

public class JogHudRenderer
{
    private static final int HUD_HEIGHT = 64;
    private static final int HUD_LEFT = 10;
    private static final int HUD_BASELINE_1 = 24;
    private static final int HUD_BASELINE_2 = 46;

    private final Font hudFont;
    private RGBImageUncompressed hudImage;
    private BufferedImage bufferedHud;
    private int hudWidth;
    private int hudHeight;

    public JogHudRenderer()
    {
        hudFont = new Font("Monospaced", Font.PLAIN, 16);
        hudWidth = 0;
        hudHeight = 0;
    }

    public void draw(GL4 gl, ShadersModel model)
    {
        if ( gl == null || model == null ) {
            return;
        }
        if ( !model.isShowHud() ) {
            return;
        }
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        int targetHudWidth = Math.max(1, viewport[2]);
        int targetHudHeight = Math.min(HUD_HEIGHT, Math.max(1, viewport[3]));
        ensureHudBuffers(targetHudWidth, targetHudHeight);

        String line1;
        if ( model.getRenderingMode() == ShaderOperationMode.OPENGL_4_1 ) {
            int meridians = model.getSphereMeridians();
            int parallels = model.getSphereParallels();
            int triangles = Math.max(0, (parallels - 1) * meridians * 2);
            line1 = "Number of meridians: " + meridians +
                    " Number of parallels: " + parallels +
                    " Number of triangles: " + triangles;
        }
        else {
            line1 = "Raytracing";
        }

        String line2 = (model.getRenderingMode() == ShaderOperationMode.OPENGL_4_1)
            ? "Mode [.]: GPU"
            : "Mode [.]: CPU";
        String line2Right = "Show HUD [h]";
        String lineCookMaterial = null;
        if ( model.getQuality().getShadingType() ==
             RendererConfiguration.SHADING_TYPE_COOK_TERRANCE ) {
            lineCookMaterial = "SimpleMaterial [m]: " + model.getCookTorranceMaterialLabel();
        }

        Graphics2D g = bufferedHud.createGraphics();
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, hudWidth, hudHeight);
        g.setColor(Color.WHITE);
        g.setFont(hudFont);
        g.drawString(line1, HUD_LEFT, HUD_BASELINE_1);
        g.drawString(line2, HUD_LEFT, HUD_BASELINE_2);
        int rightWidth = g.getFontMetrics().stringWidth(line2Right);
        int rightX = Math.max(HUD_LEFT, hudWidth - rightWidth - HUD_LEFT);
        g.drawString(line2Right, rightX, HUD_BASELINE_1);
        if ( lineCookMaterial != null ) {
            int materialWidth = g.getFontMetrics().stringWidth(lineCookMaterial);
            int materialX = Math.max(HUD_LEFT, hudWidth - materialWidth - HUD_LEFT);
            g.drawString(lineCookMaterial, materialX, HUD_BASELINE_2);
        }
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
        int hudY = viewport[1] + viewport[3] - this.hudHeight;

        // Save/restore viewport so HUD placement does not affect scene rendering.
        gl.glViewport(hudX, hudY, this.hudWidth, this.hudHeight);
        Jogl4ImageRenderer.draw(gl, hudImage);
        gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    public void dispose()
    {
        // No-op. OpenGL texture is recreated every draw via unload+draw.
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
