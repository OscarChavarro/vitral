package render;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.media.Calligraphic2DBuffer;

/**
A set of 2D lines (or points) sharing one drawing style, already projected
to the normalized device coordinates of a viewport (from <-1, -1> at its
lower left corner to <1, 1> at its upper right corner). It is the technology
independent output of `ProjectedLinesViewBuilder`: any 2D drawing technology
(an AWT canvas, a raster image, a plotter) can present it by mapping those
coordinates to its own pixels, as `Calligraphic2DBuffer` suggests.
*/
public class ProjectedLineLayer
{
    /**
    How the elements of a layer are drawn.
    */
    public enum Style
    {
        /** Continuous lines */
        SOLID,
        /** Dashed lines (i.e. hidden lines) */
        DASHED,
        /** Points: each element is a degenerate line, drawn as a small mark */
        POINTS
    }

    private final Calligraphic2DBuffer lines;
    private final ColorRgb color;
    private final double width;
    private final Style style;

    /**
    @param color color of every element of the layer
    @param width line width (or point size), in pixels of a standard
    resolution screen (see `ViewportElementScaler`)
    @param style how the elements are drawn
    */
    public ProjectedLineLayer(ColorRgb color, double width, Style style)
    {
        this.lines = new Calligraphic2DBuffer();
        this.color = new ColorRgb(color);
        this.width = width;
        this.style = style;
    }

    /**
    @return the lines of the layer, in normalized device coordinates
    */
    public Calligraphic2DBuffer getLines()
    {
        return lines;
    }

    public ColorRgb getColor()
    {
        return color;
    }

    public double getWidth()
    {
        return width;
    }

    public Style getStyle()
    {
        return style;
    }

    /**
    @return true if the layer has nothing to draw
    */
    public boolean isEmpty()
    {
        return lines.getNumLines() == 0;
    }
}
