#include "vsdk/toolkit/common/VSDK.h"
// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include "vsdk/toolkit/gui/XlibUtf8Text.h"
#include <math.h>
#include <stdio.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/xlib/XlibGizmo2DDrawListRenderer.h"
#include "vsdk/toolkit/gui/XFontSelection.h"
#include "vsdk/toolkit/render/xlib/XlibColorPalette.h"

static int toPixel(double value)
{
    return (int)floor(value + 0.5);
}

static int clampChannel(double value)
{
    value = value < 0 ? 0 : value > 1 ? 1 : value;
    return (int)(value * 255 + 0.5);
}

XlibGizmo2DDrawListRenderer::XlibGizmo2DDrawListRenderer()
    : display(NULL), numberOfFonts(0)
{
    int i;

    for ( i = 0; i < MAX_FONTS; i++ ) {
        fonts[i] = NULL;
        fontSizes[i] = 0;
    }
}

XlibGizmo2DDrawListRenderer::~XlibGizmo2DDrawListRenderer()
{
    releaseFonts();
}

void XlibGizmo2DDrawListRenderer::releaseFonts()
{
    int i;

    for ( i = 0; i < numberOfFonts; i++ ) {
        if ( fonts[i] != NULL && display != NULL ) {
            XFreeFontSet(display, fonts[i]);
        }
        fonts[i] = NULL;
    }
    numberOfFonts = 0;
}

void XlibGizmo2DDrawListRenderer::setDisplay(Display* d)
{
    if ( d == display ) {
        return;
    }
    releaseFonts();
    display = d;
}

/**
@return the font set of a size (Helvetica, or another font the server has,
see XFontSelection),
created on its first use; null without a display
*/
XFontSet XlibGizmo2DDrawListRenderer::font(int fontSize) const
{
    if ( display == NULL ) {
        return NULL;
    }
    int i;
    for ( i = 0; i < numberOfFonts; i++ ) {
        if ( fontSizes[i] == fontSize ) {
            return fonts[i];
        }
    }
    XFontSet set = XFontSelection::createSansFontSet(display, fontSize, FALSE);
    // The cache is not part of the state of the renderer (no mutable)
    XlibGizmo2DDrawListRenderer* self = (XlibGizmo2DDrawListRenderer*)this;
    if ( self->numberOfFonts == MAX_FONTS ) {
        // Full: the oldest one is replaced
        if ( self->fonts[0] != NULL ) {
            XFreeFontSet(display, self->fonts[0]);
        }
        for ( i = 1; i < MAX_FONTS; i++ ) {
            self->fonts[i - 1] = self->fonts[i];
            self->fontSizes[i - 1] = self->fontSizes[i];
        }
        self->numberOfFonts--;
    }
    self->fonts[self->numberOfFonts] = set;
    self->fontSizes[self->numberOfFonts] = fontSize;
    self->numberOfFonts++;
    return set;
}

int XlibGizmo2DDrawListRenderer::getTextWidth(const String& text,
                                              int fontSize) const
{
    XFontSet set = font(fontSize);
    if ( set == NULL ) {
        return (int)text.length() * fontSize / 2;
    }
    XRectangle ink;
    XRectangle logical;
    Xutf8TextExtents(set, text.c_str(), (int)text.length(),
                     &ink, &logical);
    return logical.width;
}

int XlibGizmo2DDrawListRenderer::getTextHeight(int fontSize) const
{
    XFontSet set = font(fontSize);
    if ( set == NULL ) {
        return fontSize;
    }
    XFontSetExtents* extents = XExtentsOfFontSet(set);
    return extents->max_logical_extent.height;
}

void XlibGizmo2DDrawListRenderer::draw(Drawable drawable, GC gc,
    Visual* visual, const Gizmo2DDrawList& list, int x0, int y0, int width,
    int height)
{
    if ( display == NULL || list.isEmpty() ) {
        return;
    }
    XlibColorPalette* palette = XlibColorPalette::forVisual(display, visual);
    ArrayList<Gizmo2DDrawList::Primitive> primitives =
        list.getPrimitivesInDrawingOrder();
    long i;
    int v;

    for ( i = 0; i < primitives.size(); i++ ) {
        const Gizmo2DDrawList::Primitive& p = primitives[i];
        const ColorRgb& c = p.getColor();
        unsigned long rgb = (unsigned long)clampChannel(c.r()) << 16 |
            (unsigned long)clampChannel(c.g()) << 8 |
            (unsigned long)clampChannel(c.b());
        unsigned long pixel = palette->rgbToPixel(clampChannel(c.r()),
            clampChannel(c.g()), clampChannel(c.b()));

        XSetForeground(display, gc, pixel);
        if ( p.getKind() == Gizmo2DDrawList::Kind::POLYGON ) {
            int n = p.getNumberOfVertices();
            ArrayList<XPoint> points;
            points.reserve(n + 1);
            for ( v = 0; v <= n; v++ ) {
                XPoint point;
                point.x = (short)(x0 + toPixel(p.getX(v % n)));
                point.y = (short)(y0 + toPixel(p.getY(v % n)));
                points.add(point);
            }
            if ( p.getOpacity() >= 1.0 ) {
                XFillPolygon(display, drawable, gc, points.data(), n,
                             Complex, CoordModeOrigin);
                if ( !p.isOverlay() ) {
                    XSetLineAttributes(display, gc, 1, LineSolid, CapRound,
                                       JoinRound);
                    XDrawLines(display, drawable, gc, points.data(), n + 1,
                               CoordModeOrigin);
                }
            }
            else if ( p.getOpacity() > 0.0 ) {
                fillTranslucentPolygon(drawable, gc, visual, points.data(),
                    n, rgb, p.getOpacity(), x0, y0, width, height);
            }
        }
        else if ( p.getKind() == Gizmo2DDrawList::Kind::LINE ) {
            int lineWidth = toPixel(p.getWidth());
            XSetLineAttributes(display, gc, lineWidth < 1 ? 1 : lineWidth,
                               LineSolid, CapRound, JoinRound);
            XDrawLine(display, drawable, gc,
                      x0 + toPixel(p.getX(0)), y0 + toPixel(p.getY(0)),
                      x0 + toPixel(p.getX(1)), y0 + toPixel(p.getY(1)));
        }
        else if ( p.getKind() == Gizmo2DDrawList::Kind::LABEL ) {
            XFontSet set = font(p.getFontSize());
            int textHeight = getTextHeight(p.getFontSize());
            double left = x0 + p.getX(0);
            double bottom = y0 + p.getY(0);
            const String& text = p.getText();

            if ( p.getAlignment() == Gizmo2DDrawList::Alignment::CENTER ) {
                left -= getTextWidth(text, p.getFontSize()) / 2.0;
                bottom += textHeight / 2.0;
            }
            if ( set != NULL ) {
                XFontSetExtents* extents = XExtentsOfFontSet(set);
                int ascent = -extents->max_logical_extent.y;
                Xutf8DrawString(display, drawable, set, gc, toPixel(left),
                    toPixel(bottom) - textHeight + ascent, text.c_str(),
                    (int)text.length());
            }
        }
    }
    XSetLineAttributes(display, gc, 1, LineSolid, CapButt, JoinMiter);
}

/**
Blends a polygon with the pixels of the drawable below it: the server
rasterizes its coverage in a bitmap of its bounding box (clipped to the
viewport) and the client mixes the colors where the bitmap is set.
*/
void XlibGizmo2DDrawListRenderer::fillTranslucentPolygon(Drawable drawable,
    GC gc, Visual* visual, XPoint* points, int count, unsigned long rgb,
    double opacity, int clipX, int clipY, int clipWidth,
    int clipHeight) const
{
    if ( count < 3 ) {
        return;
    }
    int minX = points[0].x;
    int maxX = points[0].x;
    int minY = points[0].y;
    int maxY = points[0].y;
    int i;
    for ( i = 1; i < count; i++ ) {
        minX = points[i].x < minX ? points[i].x : minX;
        maxX = points[i].x > maxX ? points[i].x : maxX;
        minY = points[i].y < minY ? points[i].y : minY;
        maxY = points[i].y > maxY ? points[i].y : maxY;
    }
    minX = minX < clipX ? clipX : minX;
    minY = minY < clipY ? clipY : minY;
    maxX = maxX >= clipX + clipWidth ? clipX + clipWidth - 1 : maxX;
    maxY = maxY >= clipY + clipHeight ? clipY + clipHeight - 1 : maxY;
    if ( maxX < minX || maxY < minY ) {
        return;
    }
    unsigned int w = (unsigned int)(maxX - minX + 1);
    unsigned int h = (unsigned int)(maxY - minY + 1);

    //- Coverage of the polygon -------------------------------------------
    Pixmap mask = XCreatePixmap(display, drawable, w, h, 1);
    GC maskGc = XCreateGC(display, mask, 0, NULL);
    XSetForeground(display, maskGc, 0);
    XFillRectangle(display, mask, maskGc, 0, 0, w, h);
    XSetForeground(display, maskGc, 1);
    ArrayList<XPoint> shifted;
    shifted.reserve(count);
    for ( i = 0; i < count; i++ ) {
        XPoint point;
        point.x = (short)(points[i].x - minX);
        point.y = (short)(points[i].y - minY);
        shifted.add(point);
    }
    XFillPolygon(display, mask, maskGc, shifted.data(), count, Complex,
                 CoordModeOrigin);
    XImage* coverage = XGetImage(display, mask, 0, 0, w, h, 1, XYPixmap);
    XFreeGC(display, maskGc);
    XFreePixmap(display, mask);
    if ( coverage == NULL ) {
        return;
    }

    //- Blending ----------------------------------------------------------
    XImage* below = XGetImage(display, drawable, minX, minY, w, h, AllPlanes,
                              ZPixmap);
    if ( below == NULL ) {
        XDestroyImage(coverage);
        return;
    }
    XlibColorPalette* palette = XlibColorPalette::forVisual(display, visual);
    // Indexed visuals: the colors of the pixels below come from the colormap
    palette->refreshColors();
    int r = (int)(rgb >> 16 & 0xFF);
    int g = (int)(rgb >> 8 & 0xFF);
    int b = (int)(rgb & 0xFF);
    unsigned int row;
    unsigned int column;
    for ( row = 0; row < h; row++ ) {
        for ( column = 0; column < w; column++ ) {
            if ( !XGetPixel(coverage, column, row) ) {
                continue;
            }
            int pixelRgb[3];
            palette->pixelToRgb(XGetPixel(below, column, row), pixelRgb);
            int pr = pixelRgb[0];
            int pg = pixelRgb[1];
            int pb = pixelRgb[2];
            pr = (int)(pr + (r - pr) * opacity + 0.5);
            pg = (int)(pg + (g - pg) * opacity + 0.5);
            pb = (int)(pb + (b - pb) * opacity + 0.5);
            XPutPixel(below, column, row, palette->rgbToPixel(pr, pg, pb,
                (int)(minX + column), (int)(minY + row)));
        }
    }
    XPutImage(display, drawable, gc, below, 0, 0, minX, minY, w, h);
    XDestroyImage(below);
    XDestroyImage(coverage);
}
