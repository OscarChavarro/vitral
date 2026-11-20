package render;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;

/**
A filled 2D polygon (maybe with holes, and with several main contours),
already projected to the normalized device coordinates of a viewport (from
<-1, -1> at its lower left corner to <1, 1> at its upper right corner). It
is the technology independent output of `ProjectedSurfacesViewBuilder`, as
`ProjectedLineLayer` is for lines: any 2D drawing technology can present it
by mapping those coordinates to its own pixels. Its contours are filled with
the even-odd rule.
*/
public class ProjectedPolygon
{
    private final Polygon2D area;
    private final ColorRgb color;

    /**
    @param area contours of the polygon, in normalized device coordinates
    @param color fill color
    */
    public ProjectedPolygon(Polygon2D area, ColorRgb color)
    {
        this.area = area;
        this.color = new ColorRgb(color);
    }

    public Polygon2D getArea()
    {
        return area;
    }

    public ColorRgb getColor()
    {
        return color;
    }
}
