#ifndef __PROJECTED_POLYGON_SCAN_CONVERTER__
#define __PROJECTED_POLYGON_SCAN_CONVERTER__

#include "java/util/ArrayList.h"

class ProjectedPolygon;

/**
Scan conversion of `ProjectedPolygon`s on the pixels of a viewport, for the
2D technologies that can only fill rectangles (or horizontal spans): every
contour of a polygon is filled at once with the even-odd rule, sampling the
pixel centers (a pixel is inside if its center is, with half open edges), as
the not antialiased polygon filling of AWT does. So the polygons of a hidden
surface drawing, which do not overlap and share their edges, tile the
viewport without gaps nor pixels drawn twice.

C++ port: the Java port fills the polygons with `java.awt.Graphics2D`
(`AwtProjectedPolygonRenderer`).
*/
class ProjectedPolygonScanConverter {
public:
    /**
    Receives the spans of pixels to fill.
    */
    class SpanSink {
    public:
        virtual ~SpanSink() {}
        /**
        Fills the pixels [x0, x1) of row y.
        */
        virtual void fillSpan(int x0, int x1, int y) = 0;
    };

    /**
    Converts the normalized device coordinates of a polygon to the pixels of
    a viewport (from <x, y> at its upper left corner, of the given size)
    and gives its spans, clipped to the viewport.
    */
    static void fill(const ProjectedPolygon& polygon, int x, int y,
                     int width, int height, SpanSink& sink);
};

#endif
