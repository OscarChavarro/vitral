package model;

import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;

public class RenderTransform
{
    private static final double EPSILON = 1e-9;
    private static final int DEFAULT_IMAGE_MARGIN = 10;

    private final double minX;
    private final double minY;
    private final double scale;
    private final double offsetX;
    private final double offsetY;

    public RenderTransform(double minX, double minY, double scale,
            double offsetX, double offsetY)
    {
        this.minX = minX;
        this.minY = minY;
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public static RenderTransform compute(Polygon2D polygon, PolygonModel model)
    {
        Bounds bounds = computeBounds(polygon);
        double minX = bounds.minX;
        double minY = bounds.minY;
        double polygonWidth = bounds.maxX - bounds.minX;
        double polygonHeight = bounds.maxY - bounds.minY;
        double usableWidth = model.getZoneWidth() - 1.0 - 2.0 * DEFAULT_IMAGE_MARGIN;
        double usableHeight = model.getZoneHeight() - 1.0 - 2.0 * DEFAULT_IMAGE_MARGIN;
        double scale = (polygonWidth < EPSILON || polygonHeight < EPSILON) ? 1.0
            : Math.min(usableWidth / polygonWidth, usableHeight / polygonHeight);
        double scaledWidth = polygonWidth * scale;
        double scaledHeight = polygonHeight * scale;
        double offsetX = DEFAULT_IMAGE_MARGIN + (usableWidth - scaledWidth) / 2.0;
        double offsetY = DEFAULT_IMAGE_MARGIN + (usableHeight - scaledHeight) / 2.0;

        return new RenderTransform(minX, minY, scale, offsetX, offsetY);
    }

    private static Bounds computeBounds(Polygon2D polygon)
    {
        Bounds bounds = new Bounds();
        for ( _Polygon2DContour contour : polygon.loops ) {
            for ( Vertex2D vertex : contour.vertices ) {
                bounds.include(vertex);
            }
        }
        return bounds;
    }

    public double getMinX()
    {
        return minX;
    }

    public double getMinY()
    {
        return minY;
    }

    public double getScale()
    {
        return scale;
    }

    public double getOffsetX()
    {
        return offsetX;
    }

    public double getOffsetY()
    {
        return offsetY;
    }

    private static class Bounds
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        void include(Vertex2D vertex)
        {
            minX = Math.min(minX, vertex.x);
            minY = Math.min(minY, vertex.y);
            maxX = Math.max(maxX, vertex.x);
            maxY = Math.max(maxY, vertex.y);
        }
    }
}
