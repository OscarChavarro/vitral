package render.awt;

// AWT classes
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.List;

// VSDK classes
import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;

// Application classes
import render.ProjectedPolygon;

/**
Fills `ProjectedPolygon`s with the 2D operations of AWT (`Graphics2D`): the
mapping from normalized device coordinates to the pixels of a viewport
[FOLE1992].6, as `AwtProjectedLineLayerRenderer` does for lines. The
polygons are filled without antialiasing, so polygons sharing an edge cover
its pixels once, without the seams that blending each one with the
background would leave.
*/
public class AwtProjectedPolygonRenderer
{
    private AwtProjectedPolygonRenderer()
    {
    }

    /**
    @param g graphics where to draw
    @param polygons polygons to fill, in order
    @param x0 left pixel of the viewport
    @param y0 top pixel of the viewport
    @param width width of the viewport, in pixels
    @param height height of the viewport, in pixels
    */
    public static void draw(Graphics2D g, List<ProjectedPolygon> polygons,
                            int x0, int y0, int width, int height)
    {
        Object antialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        for ( ProjectedPolygon polygon : polygons ) {
            Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            for ( _Polygon2DContour loop : polygon.getArea().loops ) {
                boolean first = true;
                for ( Vertex2D v : loop.vertices ) {
                    double x = AwtProjectedLineLayerRenderer.toPixelX(v.x, x0, width);
                    double y = AwtProjectedLineLayerRenderer.toPixelY(v.y, y0, height);
                    if ( first ) {
                        path.moveTo(x, y);
                        first = false;
                    }
                    else {
                        path.lineTo(x, y);
                    }
                }
                if ( !first ) {
                    path.closePath();
                }
            }
            g.setColor(AwtProjectedLineLayerRenderer.toAwtColor(polygon.getColor()));
            g.fill(path);
        }
        if ( antialiasing != null ) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
        }
    }
}
