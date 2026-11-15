package vsdk.toolkit.render.awt;

// AWT classes
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.Map;

// VitralSDK classes
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.render.gizmo.Gizmo2DDrawList;
import vsdk.toolkit.render.gizmo.InputGizmo2DPainter;

/**
Draws a `Gizmo2DDrawList` (the 2D presentation of the gizmos, computed by the
painters of `vsdk.toolkit.render.gizmo`) with the 2D operations of AWT
(`Graphics2D`), so gizmos can be shown over a viewport without any 3D API.

Opaque polygons coming from 3D elements are also outlined with a thin stroke
of their own color, so the seams antialiasing leaves between neighbor
polygons are not seen. Labels use the same font as the label images of
`AwtSystem.calculateLabelImage`, so the layout of the input gizmo matches
the one of the OpenGL presentation; `TEXT_MEASURER` measures them.
*/
public class AwtGizmo2DDrawListRenderer extends AwtRenderer
{
    private static final String FONT_NAME = "Arial";
    private static final float SEAM_STROKE_WIDTH = 1.0f;
    private static final FontRenderContext MEASURE_CONTEXT =
        new FontRenderContext(new AffineTransform(), true, true);
    private static final Map<Integer, Font> FONTS = new HashMap<Integer, Font>();

    /**
    Measures texts as `draw` presents the labels of a draw list.
    */
    public static final InputGizmo2DPainter.TextMeasurer TEXT_MEASURER =
        new InputGizmo2DPainter.TextMeasurer() {
            @Override
            public int getTextWidth(String text, int fontSize) {
                return (int)Math.ceil(font(fontSize).getStringBounds(
                    text, MEASURE_CONTEXT).getWidth());
            }

            @Override
            public int getTextHeight(int fontSize) {
                return (int)Math.ceil(font(fontSize).getLineMetrics(
                    "0", MEASURE_CONTEXT).getHeight());
            }
        };

    private AwtGizmo2DDrawListRenderer()
    {
    }

    /**
    @param g graphics where to draw
    @param list primitives to draw, in pixels of the viewport
    @param x0 left pixel of the viewport in `g`
    @param y0 top pixel of the viewport in `g`
    */
    public static void draw(Graphics2D g, Gizmo2DDrawList list, int x0, int y0)
    {
        if ( list == null || list.isEmpty() ) {
            return;
        }
        Stroke previousStroke = g.getStroke();
        Font previousFont = g.getFont();
        Stroke seamStroke = new BasicStroke(SEAM_STROKE_WIDTH,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for ( Gizmo2DDrawList.Primitive p : list.getPrimitivesInDrawingOrder() ) {
            g.setColor(toAwtColor(p.getColor(), p.getOpacity()));
            switch ( p.getKind() ) {
              case POLYGON:
                Path2D.Double path = polygon(p, x0, y0);
                g.fill(path);
                if ( !p.isOverlay() && p.getOpacity() >= 1.0 ) {
                    g.setStroke(seamStroke);
                    g.draw(path);
                }
                break;
              case LINE:
                g.setStroke(new BasicStroke((float)Math.max(1.0, p.getWidth()),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(x0 + p.getX(0), y0 + p.getY(0),
                    x0 + p.getX(1), y0 + p.getY(1)));
                break;
              case LABEL:
                drawLabel(g, p, x0, y0);
                break;
              default:
                break;
            }
        }
        g.setStroke(previousStroke);
        g.setFont(previousFont);
    }

    private static Path2D.Double polygon(Gizmo2DDrawList.Primitive p, int x0, int y0)
    {
        Path2D.Double path = new Path2D.Double();

        path.moveTo(x0 + p.getX(0), y0 + p.getY(0));
        for ( int i = 1; i < p.getNumberOfVertices(); i++ ) {
            path.lineTo(x0 + p.getX(i), y0 + p.getY(i));
        }
        path.closePath();
        return path;
    }

    /**
    Draws a label whose box has the height given by `TEXT_MEASURER` and the
    baseline at its ascent from the top, as in the label images of
    `AwtSystem`.
    */
    private static void drawLabel(Graphics2D g, Gizmo2DDrawList.Primitive p,
                                  int x0, int y0)
    {
        g.setFont(font(p.getFontSize()));
        FontMetrics metrics = g.getFontMetrics();
        int height = TEXT_MEASURER.getTextHeight(p.getFontSize());
        double left = x0 + p.getX(0);
        double bottom = y0 + p.getY(0);

        if ( p.getAlignment() == Gizmo2DDrawList.Alignment.CENTER ) {
            left -= TEXT_MEASURER.getTextWidth(p.getText(), p.getFontSize()) / 2.0;
            bottom += height / 2.0;
        }
        g.drawString(p.getText(), (float)left,
            (float)(bottom - height + metrics.getAscent()));
    }

    private static Font font(int fontSize)
    {
        synchronized ( FONTS ) {
            return FONTS.computeIfAbsent(fontSize,
                size -> new Font(FONT_NAME, Font.PLAIN, size));
        }
    }

    private static Color toAwtColor(ColorRgb c, double opacity)
    {
        return new Color(clamp(c.r()), clamp(c.g()), clamp(c.b()), clamp(opacity));
    }

    private static float clamp(double value)
    {
        return (float)Math.max(0.0, Math.min(1.0, value));
    }
}
