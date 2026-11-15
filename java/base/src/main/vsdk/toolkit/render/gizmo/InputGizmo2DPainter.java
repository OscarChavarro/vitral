package vsdk.toolkit.render.gizmo;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.gui.gizmo.InputGizmo;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

/**
Presents an {@link InputGizmo} as 2D overlay primitives (see
`Gizmo2DDrawList`), with the layout of `Jogl4InputGizmoRenderer`: at the
lower right corner of the viewport, each number inside a frame with the color
of its box (see `InputGizmo.getFieldDisplayColor`), the frames at least as
wide as the reference text of the gizmo, and a thin line below the frame of
the selected box. Sizes follow the screen resolution (see
`ViewportElementScaler`).

The width of the texts depends on the fonts of the drawing technology, so
it is measured by a `TextMeasurer` given by the backend.
*/
public class InputGizmo2DPainter
{
    /**
    Measures texts as the backend draws `Gizmo2DDrawList` labels.
    */
    public interface TextMeasurer
    {
        /**
        @return the width, in pixels, of the box of a text
        */
        int getTextWidth(String text, int fontSize);

        /**
        @return the height, in pixels, of the box of any text of a font size
        */
        int getTextHeight(int fontSize);
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

    private InputGizmo2DPainter()
    {
    }

    /**
    @param out list receiving the primitives
    @param gizmo gizmo to present
    @param viewportWidth width of the viewport, in pixels
    @param viewportHeight height of the viewport, in pixels
    @param scaler sizes of the elements for the resolution of the screen
    @param measurer measures the texts as the backend draws them
    */
    public static void paint(Gizmo2DDrawList out, InputGizmo gizmo,
                             int viewportWidth, int viewportHeight,
                             ViewportElementScaler scaler, TextMeasurer measurer)
    {
        if ( out == null || gizmo == null || gizmo.getNumberOfFields() <= 0 ) {
            return;
        }
        int count = gizmo.getNumberOfFields();
        int fontSize = scaler.scaleSize(BASE_FONT_SIZE);
        int margin = scaler.scaleSize(BASE_MARGIN);
        int gap = scaler.scaleSize(BASE_GAP);
        int paddingX = scaler.scaleSize(BASE_PADDING_X);
        int paddingY = scaler.scaleSize(BASE_PADDING_Y);
        int frameThickness = Math.max(1, scaler.scaleSize(BASE_FRAME_THICKNESS));
        int underlineDistance = scaler.scaleSize(BASE_UNDERLINE_DISTANCE);
        int underlineThickness = Math.max(1, scaler.scaleSize(BASE_UNDERLINE_THICKNESS));
        int referenceWidth = measurer.getTextWidth(gizmo.getReferenceText(), fontSize);
        int textHeight = measurer.getTextHeight(fontSize);

        //-----------------------------------------------------------------
        ColorRgb[] colors = new ColorRgb[count];
        String[] texts = new String[count];
        int[] textWidths = new int[count];
        int[] frameWidths = new int[count];
        int totalWidth = (count - 1) * gap;

        for ( int i = 0; i < count; i++ ) {
            colors[i] = gizmo.getFieldDisplayColor(i);
            // An empty text (i.e. all its characters deleted) is a blank box
            texts[i] = gizmo.getDisplayText(i).isEmpty() ? " " : gizmo.getDisplayText(i);
            textWidths[i] = measurer.getTextWidth(texts[i], fontSize);
            frameWidths[i] = Math.max(referenceWidth, textWidths[i]) +
                2*(paddingX + frameThickness);
            totalWidth += frameWidths[i];
        }

        int frameHeight = textHeight + 2*(paddingY + frameThickness);
        int x = viewportWidth - margin - totalWidth;
        int top = viewportHeight - margin - frameHeight;

        for ( int i = 0; i < count; i++ ) {
            addFrame(out, x, top, frameWidths[i], frameHeight, frameThickness, colors[i]);
            if ( i == gizmo.getSelectedField() ) {
                out.addOverlayRectangle(x, top + frameHeight + underlineDistance,
                    frameWidths[i], underlineThickness, colors[i]);
            }
            out.addOverlayLabel(x + (frameWidths[i] - textWidths[i]) / 2,
                top + frameHeight - frameThickness - paddingY, texts[i], colors[i],
                fontSize, Gizmo2DDrawList.Alignment.LOWER_LEFT);
            x += frameWidths[i] + gap;
        }
    }

    private static void addFrame(Gizmo2DDrawList out, int x, int y, int width,
                                 int height, int thickness, ColorRgb c)
    {
        out.addOverlayRectangle(x, y, width, thickness, c);
        out.addOverlayRectangle(x, y + height - thickness, width, thickness, c);
        out.addOverlayRectangle(x, y + thickness, thickness, height - 2*thickness, c);
        out.addOverlayRectangle(x + width - thickness, y + thickness, thickness,
            height - 2*thickness, c);
    }
}
