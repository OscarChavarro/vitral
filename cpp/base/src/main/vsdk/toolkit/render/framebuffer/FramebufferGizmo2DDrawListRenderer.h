#ifndef __FRAMEBUFFER_GIZMO_2D_DRAW_LIST_RENDERER__
#define __FRAMEBUFFER_GIZMO_2D_DRAW_LIST_RENDERER__

#include <stdint.h>

#include "vsdk/toolkit/render/gizmo/InputGizmo2DPainter.h"

class ColorRgb;
class Framebuffer;
class Gizmo2DDrawList;

/**
Draws a `Gizmo2DDrawList` (the 2D presentation of the gizmos, computed by
the painters of `vsdk/toolkit/render/gizmo`) with the operations of a
`Framebuffer`, the drawing surface of the framebuffer GUI, so gizmos can be
shown over a viewport on any of its backends (Xlib, svgalib, BGI...) without
a 3D API. It is the counterpart of the Java `AwtGizmo2DDrawListRenderer`.

- Polygons are filled with `Framebuffer::fillPolygon`, which also draws
  their edges, so neighbor polygons leave no seams.
- Translucent polygons are blended with the pixels below them when the
  backend can read its pixels back (`Framebuffer::readImage`); otherwise
  (i.e. svgalib, or palette modes without read back) they are drawn with
  an ordered dithering of their opacity (a 4x4 Bayer threshold matrix).
- Lines wider than one pixel are filled as quads.
- Labels use the only font of the framebuffer: their font size is ignored,
  and `TextMeasurer` measures them with it.
*/
class FramebufferGizmo2DDrawListRenderer {
public:
    /**
    Measures texts as `draw` presents the labels of a draw list on a
    surface.
    */
    class TextMeasurer : public InputGizmo2DPainter::TextMeasurer {
    private:
        const Framebuffer& surface;
    public:
        explicit TextMeasurer(const Framebuffer& surface);
        int getTextWidth(const java::String& text, int fontSize) const override;
        int getTextHeight(int fontSize) const override;
    };

private:
    FramebufferGizmo2DDrawListRenderer();

    static void fillTranslucentPolygon(Framebuffer& surface, const int* xy,
                                       int count, uint32_t color,
                                       double opacity);

public:
    /**
    @param surface surface where to draw
    @param list primitives to draw, in pixels of the viewport
    @param x0 left pixel of the viewport in the surface
    @param y0 top pixel of the viewport in the surface
    */
    static void draw(Framebuffer& surface, const Gizmo2DDrawList& list,
                     int x0, int y0);

    /**
    @return the packed 0xRRGGBB value of a color
    */
    static uint32_t packed(const ColorRgb& color);
};

#endif
