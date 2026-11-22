//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#include "render/xlib/XlibProjectedPolygonRenderer.h"

// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.h"

static double toPixelX(double ndcX, int x0, int width)
{
    return x0 + (ndcX + 1.0) / 2.0 * width;
}

static double toPixelY(double ndcY, int y0, int height)
{
    return y0 + (1.0 - ndcY) / 2.0 * height;
}

static int toChannel(double value)
{
    return ((int)(Math::max(0.0, Math::min(1.0, value)) * 255));
}

static XPoint toPoint(const Vertex2D& v, int x0, int y0, int width,
                      int height)
{
    XPoint point;
    point.x = ((short)Math::floor(toPixelX(v.x, x0, width) + 0.5));
    point.y = ((short)Math::floor(toPixelY(v.y, y0, height) + 0.5));
    return point;
}

static unsigned long toPixel(Visual* visual, const ColorRgb& color)
{
    return XlibRGBImageUncompressedRenderer::rgbToPixel(visual,
        toChannel(color.r()), toChannel(color.g()), toChannel(color.b()));
}

/**
Builds the contours of a polygon, in pixels of the viewport, as one path:
each contour is closed and followed by the first vertex of the polygon (the
bridge joining the contours for `XFillPolygon`).
@param path [out] the path
@param loopStarts [out] index in the path of the start of each contour,
followed by the size of the path: the contour `i`, closed, goes from
`loopStarts[i]` to `loopStarts[i + 1] - 2`
*/
static void toPath(ProjectedPolygon* polygon, int x0, int y0, int width,
                   int height, ArrayList<XPoint>& path,
                   ArrayList<int>& loopStarts)
{
    Polygon2D* area = polygon->getArea();
    XPoint start;
    BOOLEAN started = FALSE;
    long j;
    long k;

    start.x = 0;
    start.y = 0;

    for ( j = 0; j < area->loops.size(); j++ ) {
        _Polygon2DContour* loop = area->loops[j];
        if ( loop == NULL || loop->vertices.size() == 0 ) {
            continue;
        }
        XPoint first = toPoint(loop->vertices[0], x0, y0, width, height);
        if ( !started ) {
            start = first;
            started = TRUE;
        }
        loopStarts.add((int)path.size());
        for ( k = 0; k < loop->vertices.size(); k++ ) {
            path.add(toPoint(loop->vertices[k], x0, y0, width, height));
        }
        path.add(first);
        path.add(start);
    }
    loopStarts.add((int)path.size());
}

XlibProjectedPolygonRenderer::XlibProjectedPolygonRenderer()
{
}

void XlibProjectedPolygonRenderer::draw(Display* display, Drawable frame,
    GC gc, Visual* visual, const ArrayList<ProjectedPolygon*>& polygons,
    int x0, int y0, int width, int height)
{
    long i;

    XSetFillRule(display, gc, EvenOddRule);
    for ( i = 0; i < polygons.size(); i++ ) {
        ArrayList<XPoint> path;
        ArrayList<int> loopStarts;

        toPath(polygons[i], x0, y0, width, height, path, loopStarts);
        if ( path.size() < 3 ) {
            continue;
        }
        XSetForeground(display, gc, toPixel(visual, polygons[i]->getColor()));
        XFillPolygon(display, frame, gc, &path[0], (int)path.size(), Complex,
                     CoordModeOrigin);
    }
}

void XlibProjectedPolygonRenderer::drawContours(Display* display,
    Drawable frame, GC gc, Visual* visual,
    const ArrayList<ProjectedPolygon*>& polygons,
    int x0, int y0, int width, int height,
    float lineWidth, const ColorRgb* lineColor)
{
    XGCValues previousStroke;
    long i;
    long j;

    XGetGCValues(display, gc,
        GCLineWidth | GCLineStyle | GCCapStyle | GCJoinStyle,
        &previousStroke);

    XSetLineAttributes(display, gc,
        ((unsigned int)Math::floor(lineWidth + 0.5)), LineSolid, CapRound,
        JoinRound);
    if ( lineColor != NULL ) {
        XSetForeground(display, gc, toPixel(visual, *lineColor));
    }
    for ( i = 0; i < polygons.size(); i++ ) {
        ArrayList<XPoint> path;
        ArrayList<int> loopStarts;

        if ( lineColor == NULL ) {
            XSetForeground(display, gc,
                toPixel(visual, polygons[i]->getColor()));
        }
        toPath(polygons[i], x0, y0, width, height, path, loopStarts);
        for ( j = 0; j + 1 < loopStarts.size(); j++ ) {
            // Without the bridge to the first vertex of the polygon
            XDrawLines(display, frame, gc, &path[loopStarts[j]],
                loopStarts[j + 1] - loopStarts[j] - 1, CoordModeOrigin);
        }
    }
    XSetLineAttributes(display, gc, previousStroke.line_width,
        previousStroke.line_style, previousStroke.cap_style,
        previousStroke.join_style);
}
