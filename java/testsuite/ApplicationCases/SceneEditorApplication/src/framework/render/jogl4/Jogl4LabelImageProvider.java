package framework.render.jogl4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.media.RGBAImageUncompressed;

/**
Creates the images used by JOGL renderers to draw text labels (viewport
titles, axis names). It isolates the rendering classes from the technology
used to rasterize text (i.e. AWT fonts), which is provided by the caller.
*/
public interface Jogl4LabelImageProvider
{
    /**
    @param text the text to draw
    @param color the color of the text
    @return an image with the text on transparent background
    */
    RGBAImageUncompressed createLabelImage(String text, ColorRgb color);
}
