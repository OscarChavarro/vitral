package render.awt;

// AWT classes
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

// VSDK classes
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.Calligraphic2DBuffer;

// Application classes
import render.ProjectedLineLayer;

/**
Draws `ProjectedLineLayer`s with the 2D operations of AWT (`Graphics2D`): the
last stage of the calligraphic pipeline, the mapping from normalized device
coordinates to the pixels of a viewport [FOLE1992].6, as
`AwtCalligraphic2DBufferRenderer` does, but honoring the color, width and
style of each layer and with sub-pixel (antialiased) precision.
*/
public class AwtProjectedLineLayerRenderer
{
    /// Distance, in pixels, under which a segment continues the previous one
    private static final double CHAIN_TOLERANCE = 0.01;

    private AwtProjectedLineLayerRenderer()
    {
    }

    /**
    @param g graphics where to draw
    @param layers layers to draw, in order
    @param x0 left pixel of the viewport
    @param y0 top pixel of the viewport
    @param width width of the viewport, in pixels
    @param height height of the viewport, in pixels
    @param widthScale factor applied to the widths of the layers, given for a
    standard resolution screen (see `ViewportElementScaler`)
    */
    public static void draw(Graphics2D g, List<ProjectedLineLayer> layers,
                            int x0, int y0, int width, int height,
                            double widthScale)
    {
        Stroke previousStroke = g.getStroke();

        for ( ProjectedLineLayer layer : layers ) {
            float lineWidth = (float)Math.max(1.0, layer.getWidth() * widthScale);
            g.setColor(toAwtColor(layer.getColor()));
            switch ( layer.getStyle() ) {
              case DASHED:
                g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f,
                    new float[] {4.0f * lineWidth, 3.0f * lineWidth}, 0.0f));
                drawLines(g, layer.getLines(), x0, y0, width, height);
                break;
              case POINTS:
                drawPoints(g, layer.getLines(), x0, y0, width, height, lineWidth);
                break;
              case SOLID:
              default:
                g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
                drawLines(g, layer.getLines(), x0, y0, width, height);
                break;
            }
        }
        g.setStroke(previousStroke);
    }

    /**
    Consecutive segments where one starts at the end of the previous one (i.e.
    the polyline approximating a curve, or a line broken by the hidden line
    algorithm) are drawn as one path, so the pattern of a dashed stroke
    continues along it instead of starting again at each (maybe very short)
    segment, which would draw it as a continuous line.
    */
    private static void drawLines(Graphics2D g, Calligraphic2DBuffer lines,
                                  int x0, int y0, int width, int height)
    {
        Path2D.Double path = new Path2D.Double();
        double lastX = Double.NaN;
        double lastY = Double.NaN;
        int i;

        for ( i = 0; i < lines.getNumLines(); i++ ) {
            Vector3Dd[] segment = lines.get2DLine(i);
            double ax = toPixelX(segment[0].x(), x0, width);
            double ay = toPixelY(segment[0].y(), y0, height);
            double bx = toPixelX(segment[1].x(), x0, width);
            double by = toPixelY(segment[1].y(), y0, height);

            if ( Math.abs(ax - lastX) > CHAIN_TOLERANCE ||
                 Math.abs(ay - lastY) > CHAIN_TOLERANCE ||
                 Double.isNaN(lastX) ) {
                path.moveTo(ax, ay);
            }
            path.lineTo(bx, by);
            lastX = bx;
            lastY = by;
        }
        g.draw(path);
    }

    private static void drawPoints(Graphics2D g, Calligraphic2DBuffer lines,
                                   int x0, int y0, int width, int height,
                                   double size)
    {
        Rectangle2D.Double mark = new Rectangle2D.Double();
        int i;

        for ( i = 0; i < lines.getNumLines(); i++ ) {
            Vector3Dd[] segment = lines.get2DLine(i);
            mark.setRect(toPixelX(segment[0].x(), x0, width) - size / 2.0,
                toPixelY(segment[0].y(), y0, height) - size / 2.0, size, size);
            g.fill(mark);
        }
    }

    /**
    @param ndcX horizontal normalized device coordinate (-1 is the left side)
    */
    private static double toPixelX(double ndcX, int x0, int width)
    {
        return x0 + (ndcX + 1.0) / 2.0 * width;
    }

    /**
    @param ndcY vertical normalized device coordinate (-1 is the bottom side,
    while AWT pixels grow downwards)
    */
    private static double toPixelY(double ndcY, int y0, int height)
    {
        return y0 + (1.0 - ndcY) / 2.0 * height;
    }

    /**
    @param color a vitral color, with components between 0 and 1
    @return the same AWT color
    */
    public static Color toAwtColor(ColorRgb color)
    {
        return new Color(clamp(color.r()), clamp(color.g()), clamp(color.b()));
    }

    private static float clamp(double value)
    {
        return (float)Math.max(0.0, Math.min(1.0, value));
    }
}
