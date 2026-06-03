package render;

import java.util.ArrayList;
import java.util.List;

import model.PolygonModel;
import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.render.Rasterizer2D;

public class TriangleRasterizer
{
    private static final byte[][] TRIANGLE_FILL_PALETTE = {
        {(byte)210, (byte)90,  (byte)90},
        {(byte)90,  (byte)200, (byte)90},
        {(byte)90,  (byte)90,  (byte)220},
        {(byte)210, (byte)200, (byte)80},
        {(byte)80,  (byte)200, (byte)200},
        {(byte)200, (byte)80,  (byte)200},
        {(byte)200, (byte)140, (byte)60},
        {(byte)140, (byte)80,  (byte)200},
    };

    public void renderTriangulatedPolygon(RGBImageUncompressed image,
            PolygonModel model,
            List<MonotoneDecompositionTriangulator.Triangle> triangles,
            double minX, double minY, double scale, double offsetX,
            double offsetY, RGBPixel borderColor)
    {
        List<Vertex2D> polygonVertices = flattenVertices(model.getPolygon2D());
        RGBPixel edgeColor = new RGBPixel();
        edgeColor.r = -1;
        edgeColor.g = -1;
        edgeColor.b = -1;

        RGBPixel fillColor = new RGBPixel();

        for ( int i = 0; i < triangles.size(); i++ ) {
            MonotoneDecompositionTriangulator.Triangle triangle = triangles.get(i);
            if ( !isValidTriangleIndex(triangle, polygonVertices.size()) ) {
                continue;
            }

            Vertex2D vertexA = polygonVertices.get(triangle.a);
            Vertex2D vertexB = polygonVertices.get(triangle.b);
            Vertex2D vertexC = polygonVertices.get(triangle.c);

            int ax = projectX(vertexA.x, minX, scale, offsetX);
            int ay = projectY(vertexA.y, minY, scale, offsetY);
            int bx = projectX(vertexB.x, minX, scale, offsetX);
            int by = projectY(vertexB.y, minY, scale, offsetY);
            int cx = projectX(vertexC.x, minX, scale, offsetX);
            int cy = projectY(vertexC.y, minY, scale, offsetY);

            byte[] paletteEntry = TRIANGLE_FILL_PALETTE[i % TRIANGLE_FILL_PALETTE.length];
            fillColor.r = paletteEntry[0];
            fillColor.g = paletteEntry[1];
            fillColor.b = paletteEntry[2];

            fillTriangle(image, ax, ay, bx, by, cx, cy, fillColor);

            Rasterizer2D.drawLine(image, ax, ay, bx, by, edgeColor);
            Rasterizer2D.drawLine(image, bx, by, cx, cy, edgeColor);
            Rasterizer2D.drawLine(image, cx, cy, ax, ay, edgeColor);
        }

        PolygonRasterizer polygonRasterizer = new PolygonRasterizer();
        polygonRasterizer.renderPolygonBorder(image, model, minX, minY, scale,
            offsetX, offsetY, borderColor);
    }

    private static List<Vertex2D> flattenVertices(Polygon2D polygon)
    {
        List<Vertex2D> vertices = new ArrayList<>();
        for ( _Polygon2DContour contour : polygon.loops ) {
            vertices.addAll(contour.vertices);
        }
        return vertices;
    }

    private static boolean isValidTriangleIndex(
            MonotoneDecompositionTriangulator.Triangle triangle,
            int vertexCount)
    {
        return triangle.a >= 0 && triangle.b >= 0 && triangle.c >= 0 &&
            triangle.a < vertexCount && triangle.b < vertexCount &&
            triangle.c < vertexCount;
    }

    private static int projectX(double x, double minX, double scale,
            double offsetX)
    {
        return (int)(offsetX + (x - minX) * scale);
    }

    private static int projectY(double y, double minY, double scale,
            double offsetY)
    {
        return (int)(offsetY + (y - minY) * scale);
    }

    private static void fillTriangle(RGBImageUncompressed image,
            int x0, int y0, int x1, int y1, int x2, int y2, RGBPixel color)
    {
        int imageWidth = image.getXSize();
        int imageHeight = image.getYSize();
        int yMin = Math.max(0, Math.min(y0, Math.min(y1, y2)));
        int yMax = Math.min(imageHeight - 1,
            Math.max(y0, Math.max(y1, y2)));

        int[][] vertices = {{x0, y0}, {x1, y1}, {x2, y2}};

        for ( int y = yMin; y <= yMax; y++ ) {
            int xMin = Integer.MAX_VALUE;
            int xMax = Integer.MIN_VALUE;

            for ( int edgeIndex = 0; edgeIndex < 3; edgeIndex++ ) {
                int ax = vertices[edgeIndex][0];
                int ay = vertices[edgeIndex][1];
                int bx = vertices[(edgeIndex + 1) % 3][0];
                int by = vertices[(edgeIndex + 1) % 3][1];

                if ( ay == by ) {
                    if ( ay == y ) {
                        xMin = Math.min(xMin, Math.min(ax, bx));
                        xMax = Math.max(xMax, Math.max(ax, bx));
                    }
                }
                else if ( (ay <= y && y <= by) || (by <= y && y <= ay) ) {
                    double interpolationFactor = (double)(y - ay) / (by - ay);
                    int xIntersection = (int)(ax + interpolationFactor * (bx - ax));
                    xMin = Math.min(xMin, xIntersection);
                    xMax = Math.max(xMax, xIntersection);
                }
            }

            if ( xMin > xMax ) {
                continue;
            }

            xMin = Math.max(0, xMin);
            xMax = Math.min(imageWidth - 1, xMax);

            for ( int x = xMin; x <= xMax; x++ ) {
                image.putPixelRgb(x, y, color);
            }
        }
    }
}
