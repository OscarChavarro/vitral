//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#ifndef __XLIB_PROJ_POLYGON_RENDERER__
#define __XLIB_PROJ_POLYGON_RENDERER__

#include <X11/Xlib.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "render/ProjectedPolygon.h"

/**
Fills `ProjectedPolygon`s with the 2D operations of Xlib: the mapping from
normalized device coordinates to the pixels of a viewport [FOLE1992].6, as
the line drawing of `XlibDrawingAreaRenderer` does for lines. Xlib fills
without antialiasing, so polygons sharing an edge cover its pixels once.

C++ port note: this is the Xlib counterpart of the Java
`AwtProjectedPolygonRenderer`. `XFillPolygon` fills one path, so the
contours of a polygon are joined into one: each contour is closed and
reached from (and left back to) the first vertex of the polygon, and as
those bridges are traversed twice in opposite directions, they cancel under
the even-odd rule.
*/
class XlibProjectedPolygonRenderer
{
private:
    XlibProjectedPolygonRenderer();

public:
    /**
    @param display display where to draw
    @param frame drawable where to draw
    @param gc graphics context (its foreground and fill rule are changed)
    @param visual visual of the drawable, for the colors
    @param polygons polygons to fill, in order
    @param x0 left pixel of the viewport
    @param y0 top pixel of the viewport
    @param width width of the viewport, in pixels
    @param height height of the viewport, in pixels
    */
    static void draw(Display* display, Drawable frame, GC gc, Visual* visual,
                     const ArrayList<ProjectedPolygon*>& polygons,
                     int x0, int y0, int width, int height);
};

#endif
