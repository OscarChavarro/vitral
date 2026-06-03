package render;

import model.PolygonModel;
import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.render.Rasterizer2D;

public class PolygonRasterizer
{
    public void renderSmoothFilledPolygon(RGBImageUncompressed image,
            PolygonModel model, double minX, double minY, double scale,
            double offsetX, double offsetY, RGBPixel borderColor)
    {
        Polygon2D projectedPolygon = projectPolygon(model.getPolygon2D(), minX,
            minY, scale, offsetX, offsetY);

        try {
            Rasterizer2D.fillSmoothPolygon(image, projectedPolygon);
        }
        catch (Exception ignored) {
            // Some polygon shapes trigger the known quicksort issue in this helper.
        }

        Rasterizer2D.drawPolygon(image, projectedPolygon, borderColor);
    }

    public void renderPolygonBorder(RGBImageUncompressed image,
            PolygonModel model, double minX, double minY, double scale,
            double offsetX, double offsetY, RGBPixel borderColor)
    {
        Polygon2D projectedPolygon = projectPolygon(model.getPolygon2D(), minX,
            minY, scale, offsetX, offsetY);
        Rasterizer2D.drawPolygon(image, projectedPolygon, borderColor);
    }

    Polygon2D projectPolygon(Polygon2D sourcePolygon, double minX,
            double minY, double scale, double offsetX, double offsetY)
    {
        Polygon2D projectedPolygon = new Polygon2D();
        projectedPolygon.loops.clear();

        for ( _Polygon2DContour contour : sourcePolygon.loops ) {
            projectedPolygon.nextLoop();
            for ( Vertex2D vertex : contour.vertices ) {
                double projectedX = offsetX + (vertex.x - minX) * scale;
                double projectedY = offsetY + (vertex.y - minY) * scale;
                projectedPolygon.addVertex(projectedX, projectedY, 0.2, 0.6,
                    1.0);
            }
        }

        return projectedPolygon;
    }
}
