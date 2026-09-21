package vsdk.toolkit.render.jogl.viewport;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.media.RGBAImageUncompressed;

/**
Creates the images used by JOGL renderers to draw text labels (viewport
titles, axis names). It isolates the rendering classes from the technology
used to rasterize text (i.e. AWT fonts), which is provided by the caller.
*/
public interface Jogl4LabelImageProvider
{
    /** Font size, in pixels, of the labels created without giving one. */
    int DEFAULT_FONT_SIZE = 14;

    /**
    @param text the text to draw
    @param color the color of the text
    @param fontSize the size of the font, in pixels
    @return an image with the text on transparent background
    */
    RGBAImageUncompressed createLabelImage(String text, ColorRgb color,
                                           int fontSize);

    /**
    @param text the text to draw
    @param color the color of the text
    @return an image with the text with the default font size
    */
    default RGBAImageUncompressed createLabelImage(String text, ColorRgb color)
    {
        return createLabelImage(text, color, DEFAULT_FONT_SIZE);
    }
}
