package vsdk.toolkit.render.jogl.gizmo;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.gui.gizmo.InputGizmo;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;
import vsdk.toolkit.gui.viewport.ViewportSet;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4ColoredPrimitiveRenderer;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;

/**
Renders an {@link InputGizmo} over a viewport with the GL4 core pipeline, at
its lower right corner: each number is a label image inside a frame with the
color of its box (see `InputGizmo.getFieldDisplayColor`), and the selected box
has a thin line below its frame. Sizes follow the screen resolution (see
`ViewportElementScaler`).

There must be one renderer for each viewport where the gizmo is drawn, as it
keeps the label images (regenerated only when the text, the color or the font
size of a box change). Text rasterization and the texture management of the
images are provided by the `Host`, so this class depends neither on AWT nor on
the presentation of the viewport.

Usage (render thread, once per frame, with the viewport already activated):
<pre>
    renderer.draw(gl, inputGizmo);
</pre>
*/
public class Jogl4InputGizmoRenderer
{
    /**
    Services this renderer needs from who presents the viewport.
    */
    public interface Host
    {
        /**
        @param text the text to draw
        @param color the color of the text
        @param fontSize the size of the font, in pixels
        @return an image with the text on transparent background
        */
        RGBAImageUncompressed createLabelImage(String text, ColorRgb color, int fontSize);

        /**
        Draws a label image with its lower left corner at a position of the
        viewport, measured in pixels from its lower left corner.
        */
        void drawLabelImage(GL4 gl, RGBAImageUncompressed image, int x, int y);

        /**
        Marks a label image as no longer used, so its texture is released.
        */
        void discardLabelImage(RGBAImageUncompressed image);
    }

    // Sizes, in pixels, designed for legacy resolutions
    private static final int BASE_FONT_SIZE = 14;
    private static final int BASE_MARGIN = 10;
    private static final int BASE_GAP = 4;
    private static final int BASE_PADDING_X = 3;
    private static final int BASE_PADDING_Y = 1;
    private static final int BASE_FRAME_THICKNESS = 1;
    private static final int BASE_UNDERLINE_DISTANCE = 2;
    private static final int BASE_UNDERLINE_THICKNESS = 2;

    private final Host host;
    private final ViewportSet viewportSet;
    private final Viewport viewport;

    private RGBAImageUncompressed[] images;
    private String[] imageTexts;
    private ColorRgb[] imageColors;
    private int imageFontSize;
    private String referenceText;
    private int referenceTextWidth;

    /**
    @param host provider of label images and their drawing
    @param viewportSet set the viewport belongs to
    @param viewport viewport where the gizmo is drawn
    */
    public Jogl4InputGizmoRenderer(Host host, ViewportSet viewportSet, Viewport viewport)
    {
        this.host = host;
        this.viewportSet = viewportSet;
        this.viewport = viewport;
    }

    /**
    Draws the gizmo over the viewport.
    @param gl OpenGL context
    @param gizmo gizmo to draw
    */
    public void draw(GL4 gl, InputGizmo gizmo)
    {
        if ( gl == null || gizmo == null ) {
            return;
        }
        int count = gizmo.getNumberOfFields();
        ViewportElementScaler scaler = viewportSet.getElementScaler();
        int fontSize = scaler.scaleSize(BASE_FONT_SIZE);
        int margin = scaler.scaleSize(BASE_MARGIN);
        int gap = scaler.scaleSize(BASE_GAP);
        int paddingX = scaler.scaleSize(BASE_PADDING_X);
        int paddingY = scaler.scaleSize(BASE_PADDING_Y);
        int frameThickness = Math.max(1, scaler.scaleSize(BASE_FRAME_THICKNESS));
        int underlineDistance = scaler.scaleSize(BASE_UNDERLINE_DISTANCE);
        int underlineThickness = Math.max(1, scaler.scaleSize(BASE_UNDERLINE_THICKNESS));

        if ( images == null || images.length != count ) {
            discardImages();
            images = new RGBAImageUncompressed[count];
            imageTexts = new String[count];
            imageColors = new ColorRgb[count];
        }
        // The frames have at least the width of the reference text of the gizmo
        if ( fontSize != imageFontSize || referenceTextWidth == 0 ||
             !gizmo.getReferenceText().equals(referenceText) ) {
            // The reference image is only measured, it is never drawn
            referenceText = gizmo.getReferenceText();
            referenceTextWidth = host.createLabelImage(
                referenceText, InputGizmo.HIGHLIGHT_COLOR, fontSize).getXSize();
        }

        //-----------------------------------------------------------------
        ColorRgb[] colors = new ColorRgb[count];
        int[] frameWidths = new int[count];
        int totalWidth = 0;

        for ( int i = 0; i < count; i++ ) {
            colors[i] = gizmo.getFieldDisplayColor(i);
            updateImage(i, gizmo.getDisplayText(i), colors[i], fontSize);
            frameWidths[i] = Math.max(referenceTextWidth, images[i].getXSize())
                + 2*(paddingX + frameThickness);
            totalWidth += frameWidths[i];
        }
        imageFontSize = fontSize;
        totalWidth += (count - 1) * gap;

        int frameHeight = images[0].getYSize() + 2*(paddingY + frameThickness);
        int x = viewport.getPixelSizeX() - margin - totalWidth;
        int y = margin;
        List<float[]> rectangles = new ArrayList<float[]>();
        int[] frameStarts = new int[count];

        for ( int i = 0; i < count; i++ ) {
            frameStarts[i] = x;
            addFrame(rectangles, x, y, frameWidths[i], frameHeight, frameThickness, colors[i]);
            if ( i == gizmo.getSelectedField() ) {
                int underlineY = y - underlineDistance - underlineThickness;

                addRectangle(rectangles, x, underlineY, x + frameWidths[i],
                    underlineY + underlineThickness, colors[i]);
            }
            x += frameWidths[i] + gap;
        }
        drawRectangles(gl, rectangles);

        for ( int i = 0; i < count; i++ ) {
            int textX = frameStarts[i] + (frameWidths[i] - images[i].getXSize()) / 2;
            int textY = y + frameThickness + paddingY;

            host.drawLabelImage(gl, images[i], textX, textY);
        }
    }

    /**
    Deletes the OpenGL resources (label textures) of this renderer.
    PRE: the OpenGL context that created them is current.
    @param gl OpenGL context
    */
    public void disposeGlResources(GL4 gl)
    {
        if ( images == null ) {
            return;
        }
        for ( int i = 0; i < images.length; i++ ) {
            if ( images[i] != null ) {
                Jogl4ImageRenderer.unload(gl, images[i]);
                images[i] = null;
                imageTexts[i] = null;
            }
        }
    }

    private void discardImages()
    {
        if ( images == null ) {
            return;
        }
        for ( RGBAImageUncompressed image : images ) {
            if ( image != null ) {
                host.discardLabelImage(image);
            }
        }
    }

    private void updateImage(int field, String text, ColorRgb color, int fontSize)
    {
        if ( images[field] != null && fontSize == imageFontSize &&
             text.equals(imageTexts[field]) && color.equals(imageColors[field]) ) {
            return;
        }
        if ( images[field] != null ) {
            host.discardLabelImage(images[field]);
        }
        // An empty text (i.e. all its characters deleted) cannot be rasterized:
        // an empty box is drawn with a blank text
        images[field] = host.createLabelImage(text.isEmpty() ? " " : text, color, fontSize);
        imageTexts[field] = text;
        imageColors[field] = color;
    }

    //= Frames ============================================================
    // Rectangles are {x0, y0, x1, y1, r, g, b}, in pixels of the viewport,
    // measured from its lower left corner

    private static void addRectangle(List<float[]> rectangles,
                                     int x0, int y0, int x1, int y1, ColorRgb c)
    {
        rectangles.add(new float[] {x0, y0, x1, y1, (float)c.r(), (float)c.g(), (float)c.b()});
    }

    private static void addFrame(List<float[]> rectangles, int x, int y,
                                 int width, int height, int thickness, ColorRgb c)
    {
        addRectangle(rectangles, x, y, x + width, y + thickness, c);
        addRectangle(rectangles, x, y + height - thickness, x + width, y + height, c);
        addRectangle(rectangles, x, y + thickness, x + thickness, y + height - thickness, c);
        addRectangle(rectangles, x + width - thickness, y + thickness, x + width,
            y + height - thickness, c);
    }

    /**
    Draws opaque rectangles over the whole surface of the viewport set, clipped
    to the area of this viewport with the scissor test (the same way label
    images are drawn by the viewport window). The state changed is
    restored.
    */
    private void drawRectangles(GL4 gl, List<float[]> rectangles)
    {
        if ( rectangles.isEmpty() ) {
            return;
        }
        int[] currentViewport = new int[4];

        gl.glGetIntegerv(GL4.GL_VIEWPORT, currentViewport, 0);

        int surfaceWidth = viewportSet.getSizeXInPixels();
        int surfaceHeight = viewportSet.getSizeYInPixels();

        if ( surfaceWidth <= 0 || surfaceHeight <= 0 ) {
            surfaceWidth = currentViewport[0] + currentViewport[2];
            surfaceHeight = currentViewport[1] + currentViewport[3];
        }

        float[] positions = new float[rectangles.size() * 6 * 3];
        float[] colors = new float[rectangles.size() * 6 * 4];
        int vertex = 0;

        for ( float[] r : rectangles ) {
            float x0 = viewport.getPixelStartX() + r[0];
            float y0 = viewport.getPixelStartY() + r[1];
            float x1 = viewport.getPixelStartX() + r[2];
            float y1 = viewport.getPixelStartY() + r[3];
            float[] corners = new float[] {x0, y0,  x1, y0,  x1, y1,  x0, y0,  x1, y1,  x0, y1};

            for ( int k = 0; k < 6; k++ ) {
                positions[3*vertex] = 2.0f * corners[2*k] / surfaceWidth - 1.0f;
                positions[3*vertex + 1] = 2.0f * corners[2*k + 1] / surfaceHeight - 1.0f;
                positions[3*vertex + 2] = 0;
                colors[4*vertex] = r[4];
                colors[4*vertex + 1] = r[5];
                colors[4*vertex + 2] = r[6];
                colors[4*vertex + 3] = 1.0f;
                vertex++;
            }
        }

        boolean scissorWasEnabled = gl.glIsEnabled(GL4.GL_SCISSOR_TEST);
        int[] previousScissor = new int[4];

        gl.glGetIntegerv(GL4.GL_SCISSOR_BOX, previousScissor, 0);

        gl.glViewport(0, 0, surfaceWidth, surfaceHeight);
        gl.glEnable(GL4.GL_SCISSOR_TEST);
        gl.glScissor(viewport.getPixelStartX(), viewport.getPixelStartY(),
            viewport.getPixelSizeX(), viewport.getPixelSizeY());
        gl.glDisable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);

        Jogl4ColoredPrimitiveRenderer.draw(gl, Matrix4x4d.identityMatrix(),
            GL4.GL_TRIANGLES, positions, colors);

        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glScissor(previousScissor[0], previousScissor[1], previousScissor[2], previousScissor[3]);
        if ( !scissorWasEnabled ) {
            gl.glDisable(GL4.GL_SCISSOR_TEST);
        }
        gl.glViewport(currentViewport[0], currentViewport[1], currentViewport[2], currentViewport[3]);
    }
}
