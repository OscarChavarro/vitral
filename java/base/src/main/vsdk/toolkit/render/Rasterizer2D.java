//= References:                                                             =
//= [BRES1965] Bresenham, J.E. "Algorithm for computer control of a digital =
//=            plotter" IBM Syst. J. 4, 1 (1965), 25-30.                    =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =

package vsdk.toolkit.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;

public class Rasterizer2D extends RenderingElement
{
    private static final class FillEdge
    {
        int yMin;
        int yMaxExclusive;
        double xAtCurrentY;
        double inverseSlope;
        int sortOrder;
    }

    private interface SpanShader
    {
        void shade(Image img, Polygon2D polygon, int y, int xStart,
            int xEndExclusive);
    }

    private static final Comparator<FillEdge> ACTIVE_EDGE_COMPARATOR =
        new Comparator<FillEdge>() {
            @Override
            public int compare(FillEdge a, FillEdge b)
            {
                int cmp = Double.compare(a.xAtCurrentY, b.xAtCurrentY);
                if ( cmp != 0 ) {
                    return cmp;
                }

                cmp = Double.compare(a.inverseSlope, b.inverseSlope);
                if ( cmp != 0 ) {
                    return cmp;
                }

                return Integer.compare(a.sortOrder, b.sortOrder);
            }
        };

    /**
    This algorithm implements the Bresenham line algoritm with NO CLIPPING!
    See [BRES1965].
    Note that this is a currently naive implementation that makes use
    of double floating point arithmetic, while original Bresenham algorithm
    make use of most efficient integer line arithmetic.
    */
    public static void drawLine(Image img, int x0, int y0, int x1, int y1, RGBPixel p)
    {
        double dx, dy;
        double dxdy;
        double dydx;
        int x, y;
        double xx, yy;

        dx = (double)(x1-x0);
        dy = (double)(y1-y0);

        if ( Math.abs(dx) > VSDK.EPSILON && Math.abs(dy/dx) <= 1 && x1 > x0 ) {
            // Pendiente entre -1 y 1
            dydx = dy/dx;
            for ( x = x0, yy = (double)y0; x <= x1; x++ ) {
                y = (int)yy;
                if ( x >= 0 && x < img.getXSize() &&
                     y >= 0 && y < img.getYSize() ) {
                    img.putPixelRgb(x, y, p);
                }
                yy += dydx;
            }
          }
          else if ( Math.abs(dx) > VSDK.EPSILON && Math.abs(dy/dx) <= 1 && x1 < x0 ) {
            // Pendiente entre -1 y 1
            dydx = dy/dx;
            for ( x = x1, yy = (double)y1; x <= x0; x++ ) {
                y = (int)yy;
                if ( x >= 0 && x < img.getXSize() &&
                     y >= 0 && y < img.getYSize() ) {
                    img.putPixelRgb(x, y, p);
                }
                yy += dydx;
            }
          }
          else if ( Math.abs(dy) > VSDK.EPSILON && y1 > y0 ) {
            // Pendiente mayor a 1 o menor a -1
            dxdy = dx/dy;
            for ( y = y0, xx = (double)x0; y <= y1; y++ ) {
                x = (int)xx;
                if ( x >= 0 && x < img.getXSize() &&
                     y >= 0 && y < img.getYSize() ) {
                    img.putPixelRgb(x, y, p);
                }
                xx += dxdy;
            }
          }
          else if ( Math.abs(dy) > VSDK.EPSILON && y1 < y0 ) {
            // Pendiente mayor a 1 o menor a -1
            dxdy = dx/dy;
            for ( y = y1, xx = (double)x1; y <= y0; y++ ) {
                x = (int)xx;
                if ( x >= 0 && x < img.getXSize() &&
                     y >= 0 && y < img.getYSize() ) {
                    img.putPixelRgb(x, y, p);
                }
                xx += dxdy;
            }
        }
    }

    /**
    Draws a lines outline from a given polygon.
    */
    public static void drawPolygon(Image img, Polygon2D p, RGBPixel color)
    {
        Vertex2D va;
        Vertex2D vb = null;
        int i;
        int j;

        for ( i = 0; i < p.loops.size(); i++ ) {
            _Polygon2DContour contour = p.loops.get(i);
            if ( contour.vertices.size() < 2 ) {
                continue;
            }
            for ( j = 0; j < contour.vertices.size() - 1; j++ ) {
                va = contour.vertices.get(j);
                vb = contour.vertices.get(j + 1);
                drawLine(img, (int)va.x, (int)va.y,
                              (int)vb.x, (int)vb.y, color);
            }
            va = vb;
            vb = contour.vertices.get(0);
            drawLine(img, (int)va.x, (int)va.y,
                          (int)vb.x, (int)vb.y, color);
        }
    }

    private static int clamp(int value, int minValue, int maxValue)
    {
        if ( value < minValue ) {
            return minValue;
        }
        if ( value > maxValue ) {
            return maxValue;
        }
        return value;
    }

    private static void addFillEdge(List<List<FillEdge>> buckets,
        Vertex2D a, Vertex2D b, int imageHeight, int[] yRange, int sortOrder)
    {
        double dx = b.x - a.x;
        double dy = b.y - a.y;

        if ( Math.abs(dx) < VSDK.EPSILON && Math.abs(dy) < VSDK.EPSILON ) {
            return;
        }
        if ( Math.abs(dy) < VSDK.EPSILON ) {
            return;
        }

        Vertex2D top = a;
        Vertex2D bottom = b;
        if ( a.y > b.y ) {
            top = b;
            bottom = a;
            dx = -dx;
            dy = -dy;
        }

        double inverseSlope = dx / dy;
        int yMin = (int)Math.ceil(top.y);
        int yMaxExclusive = (int)Math.ceil(bottom.y);

        if ( yMin >= yMaxExclusive ) {
            return;
        }

        int clippedYMin = clamp(yMin, 0, imageHeight);
        int clippedYMaxExclusive = clamp(yMaxExclusive, 0, imageHeight);

        if ( clippedYMin >= clippedYMaxExclusive ) {
            return;
        }

        FillEdge edge = new FillEdge();
        edge.yMin = clippedYMin;
        edge.yMaxExclusive = clippedYMaxExclusive;
        edge.inverseSlope = inverseSlope;
        edge.xAtCurrentY = top.x + (((double)clippedYMin) - top.y) * inverseSlope;
        edge.sortOrder = sortOrder;

        buckets.get(clippedYMin).add(edge);
        yRange[0] = Math.min(yRange[0], clippedYMin);
        yRange[1] = Math.max(yRange[1], clippedYMaxExclusive);
    }

    private static void rasterizePolygonSpans(Image img, Polygon2D polygon,
        SpanShader shader)
    {
        int imageWidth = img.getXSize();
        int imageHeight = img.getYSize();

        if ( imageWidth <= 0 || imageHeight <= 0 ) {
            return;
        }

        List<List<FillEdge>> buckets = new ArrayList<List<FillEdge>>(imageHeight);
        int y;
        for ( y = 0; y < imageHeight; y++ ) {
            buckets.add(new ArrayList<FillEdge>());
        }

        int[] yRange = {imageHeight, 0};
        int sortOrder = 0;
        int i;
        int j;

        for ( i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            int vertexCount = contour.vertices.size();
            if ( vertexCount < 2 ) {
                continue;
            }

            for ( j = 0; j < vertexCount; j++ ) {
                Vertex2D a = contour.vertices.get(j);
                Vertex2D b = contour.vertices.get((j + 1) % vertexCount);
                addFillEdge(buckets, a, b, imageHeight, yRange, sortOrder);
                sortOrder++;
            }
        }

        if ( yRange[0] >= yRange[1] ) {
            return;
        }

        List<FillEdge> activeEdges = new ArrayList<FillEdge>();

        for ( y = yRange[0]; y < yRange[1]; y++ ) {
            List<FillEdge> bucket = buckets.get(y);
            if ( !bucket.isEmpty() ) {
                activeEdges.addAll(bucket);
            }

            for ( i = activeEdges.size() - 1; i >= 0; i-- ) {
                if ( y >= activeEdges.get(i).yMaxExclusive ) {
                    activeEdges.remove(i);
                }
            }

            if ( activeEdges.size() < 2 ) {
                for ( i = 0; i < activeEdges.size(); i++ ) {
                    activeEdges.get(i).xAtCurrentY += activeEdges.get(i).inverseSlope;
                }
                continue;
            }

            Collections.sort(activeEdges, ACTIVE_EDGE_COMPARATOR);

            for ( i = 0; i + 1 < activeEdges.size(); i += 2 ) {
                double xLeft = activeEdges.get(i).xAtCurrentY;
                double xRight = activeEdges.get(i + 1).xAtCurrentY;

                if ( xLeft > xRight ) {
                    double tmp = xLeft;
                    xLeft = xRight;
                    xRight = tmp;
                }

                int xStart = (int)Math.ceil(xLeft);
                int xEndExclusive = (int)Math.ceil(xRight);

                if ( xEndExclusive <= 0 || xStart >= imageWidth ) {
                    continue;
                }

                xStart = clamp(xStart, 0, imageWidth);
                xEndExclusive = clamp(xEndExclusive, 0, imageWidth);

                if ( xStart < xEndExclusive ) {
                    shader.shade(img, polygon, y, xStart, xEndExclusive);
                }
            }

            for ( i = 0; i < activeEdges.size(); i++ ) {
                activeEdges.get(i).xAtCurrentY += activeEdges.get(i).inverseSlope;
            }
        }
    }

    /**
    Current polygon filling rasterizer (scan-line) algorithm is a NAIVE
    implementation of the general macro-algorithm outlined at [FOLE1992].3.6.

    This implementation is working but inefficient, due to the fact that
    neither scanline coherence nor edge coherence is taken into account. This
    means that for each scanline, all polygon edges are intersected 
    analitically. This is the "brute-force technique" recommended to be
    avoided in [FOLE1992].3.6.3, but it is provided as reference to
    compare efficiency with other (clever) aproaches that makes use of
    mid-point algoritms for intersection finding, or "active edge tables"
    (AETs) for efficient edge-coherence based traversals.
    */
    public static void fillPolygon(Image img, Polygon2D p, final RGBPixel color)
    {
        rasterizePolygonSpans(img, p, new SpanShader() {
            @Override
            public void shade(Image image, Polygon2D polygon, int y, int xStart,
                int xEndExclusive)
            {
                int x;
                for ( x = xStart; x < xEndExclusive; x++ ) {
                    image.putPixelRgb(x, y, color);
                }
            }
        });
    }

    /**
    Given the current polygon `p` and the coordinate of a pixel (x, y),
    this method gives the interpolated color `outColor`.
    */
    public static void
    fillSmoothPolygonCalculateColor(
        Polygon2D p,
        double x,
        double y,
        RGBPixel outPixel)
    {
        Vertex2D va;
        int i;
        int j;
        double distance;
        double totaldistance = 0.0;

        double outR = 0.0;
        double outG = 0.0;
        double outB = 0.0;

        for ( i = 0; i < p.loops.size(); i++ ) {
            for ( j = 0; j < p.loops.get(i).vertices.size(); j++ ) {
                va = p.loops.get(i).vertices.get(j);
                distance = 1.0/(1.0+Math.sqrt((va.x-x)*(va.x-x) + 
                                              (va.y-y)*(va.y-y)));
                totaldistance += distance;
                outR += va.color.r() * distance;
                outG += va.color.g() * distance;
                outB += va.color.b() * distance;
            }
        }

        double normalizedR = outR / totaldistance;
        double normalizedG = outG / totaldistance;
        double normalizedB = outB / totaldistance;
        double clippedR = Math.max(0.0, Math.min(1.0, normalizedR));
        double clippedG = Math.max(0.0, Math.min(1.0, normalizedG));
        double clippedB = Math.max(0.0, Math.min(1.0, normalizedB));
        int rr = (int)(clippedR * 255.0);
        int gg = (int)(clippedG * 255.0);
        int bb = (int)(clippedB * 255.0);

        outPixel.r = VSDK.unsigned8BitInteger2signedByte(rr);
        outPixel.g = VSDK.unsigned8BitInteger2signedByte(gg);
        outPixel.b = VSDK.unsigned8BitInteger2signedByte(bb);
    }

    /**
    Current polygon filling rasterizer (scan-line) algorithm is a NAIVE
    implementation of the general macro-algorithm outlined at [FOLE1992].3.6.

    This implementation is working but inefficient, due to the fact that
    neither scanline coherence nor edge coherence is taken into account. This
    means that for each scanline, all polygon edges are intersected 
    analitically. This is the "brute-force technique" recommended to be
    avoided in [FOLE1992].3.6.3, but it is provided as reference to
    compare efficiency with other (clever) aproaches that makes use of
    mid-point algoritms for intersection finding, or "active edge tables"
    (AETs) for efficient edge-coherence based traversals.
    */
    public static void fillSmoothPolygon(Image img, Polygon2D p)
    {
        rasterizePolygonSpans(img, p, new SpanShader() {
            @Override
            public void shade(Image image, Polygon2D polygon, int y, int xStart,
                int xEndExclusive)
            {
                RGBPixel color = new RGBPixel();
                int x;
                for ( x = xStart; x < xEndExclusive; x++ ) {
                    fillSmoothPolygonCalculateColor(polygon, x, y, color);
                    image.putPixelRgb(x, y, color);
                }
            }
        });
    }

}
