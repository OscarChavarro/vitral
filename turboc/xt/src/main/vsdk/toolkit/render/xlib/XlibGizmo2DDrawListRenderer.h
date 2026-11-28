#ifndef __XLIB_GIZMO_2D_DRAW_LIST_RENDERER__
#define __XLIB_GIZMO_2D_DRAW_LIST_RENDERER__

#include <X11/Xlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/gizmo/InputGizmo2DPainter.h"

class Gizmo2DDrawList;

/**
Draws a `Gizmo2DDrawList` (the 2D presentation of the gizmos, computed by
the painters of `vsdk/toolkit/render/gizmo`) with the 2D operations of
Xlib, so gizmos can be shown over a viewport without any 3D API. It is the
counterpart of the Java `AwtGizmo2DDrawListRenderer`, for TrueColor and
indexed visuals (colors converted by `XlibColorPalette`).

The X11 core protocol has no translucency: translucent polygons are blended
in the client, reading the pixels below them back (`XGetImage`) over the
coverage of the polygon (filled by the server in a bitmap). Opaque polygons
coming from 3D elements are also outlined with a one pixel line of their own
color, so no seams are left between neighbor polygons. Labels use the
Helvetica font sets of the viewport titles of `XlibDrawingAreaRenderer`, one
for each font size; the renderer measures them as its `TextMeasurer`.

turboc port note: without `mutable`, the font cache is updated from the
const measuring methods through a non const `this`.
*/
class XlibGizmo2DDrawListRenderer : public InputGizmo2DPainter::TextMeasurer {
private:
    enum { MAX_FONTS = 8 };

    Display* display;
    /// Font sets created on demand, by `font`
    XFontSet fonts[MAX_FONTS];
    int fontSizes[MAX_FONTS];
    int numberOfFonts;

    XFontSet font(int fontSize) const;
    void releaseFonts();
    void fillTranslucentPolygon(Drawable drawable, GC gc, Visual* visual,
                                XPoint* points, int count,
                                unsigned long rgb, double opacity,
                                int clipX, int clipY, int clipWidth,
                                int clipHeight) const;

    XlibGizmo2DDrawListRenderer(const XlibGizmo2DDrawListRenderer&);
    XlibGizmo2DDrawListRenderer& operator=(const XlibGizmo2DDrawListRenderer&);

public:
    XlibGizmo2DDrawListRenderer();
    virtual ~XlibGizmo2DDrawListRenderer();

    /**
    Sets the display whose fonts measure and draw the labels. Call it before
    measuring texts.
    */
    void setDisplay(Display* display);

    virtual int getTextWidth(const String& text, int fontSize) const;
    virtual int getTextHeight(int fontSize) const;

    /**
    @param drawable drawable where to draw
    @param gc graphics context of the drawable (its line attributes and
    foreground are changed)
    @param visual visual of the drawable
    @param list primitives to draw, in pixels of the viewport
    @param x0 left pixel of the viewport in the drawable
    @param y0 top pixel of the viewport in the drawable
    @param width width of the viewport, which clips translucent polygons
    @param height height of the viewport, which clips translucent polygons
    */
    void draw(Drawable drawable, GC gc, Visual* visual,
              const Gizmo2DDrawList& list, int x0, int y0, int width,
              int height);
};

#endif
