#include <X11/Xutil.h>
#include <cmath>
#include <cstdio>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/xlib/XlibGizmo2DDrawListRenderer.h"
#include "vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.h"

namespace {

int toPixel(double value)
{
    return static_cast<int>(std::floor(value + 0.5));
}

int clampChannel(double value)
{
    value = value < 0 ? 0 : value > 1 ? 1 : value;
    return static_cast<int>(value * 255 + 0.5);
}

/**
@return the 8 bit value of a channel of a TrueColor pixel
*/
int channel(unsigned long value, unsigned long mask)
{
    if ( !mask ) {
        return 0;
    }
    int shift = 0;
    while ( !(mask & 1) ) {
        mask >>= 1;
        shift++;
    }
    return static_cast<int>(((value >> shift) & mask) * 255 / mask);
}

}

XlibGizmo2DDrawListRenderer::XlibGizmo2DDrawListRenderer()
    : display(nullptr), numberOfFonts(0)
{
    for ( int i = 0; i < MAX_FONTS; i++ ) {
        fonts[i] = nullptr;
        fontSizes[i] = 0;
    }
}

XlibGizmo2DDrawListRenderer::~XlibGizmo2DDrawListRenderer()
{
    releaseFonts();
}

void XlibGizmo2DDrawListRenderer::releaseFonts()
{
    for ( int i = 0; i < numberOfFonts; i++ ) {
        if ( fonts[i] != nullptr && display != nullptr ) {
            XFreeFontSet(display, fonts[i]);
        }
        fonts[i] = nullptr;
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
@return the font set of a size (Helvetica, or `fixed` when it is missing),
created on its first use; null without a display
*/
XFontSet XlibGizmo2DDrawListRenderer::font(int fontSize) const
{
    if ( display == nullptr ) {
        return nullptr;
    }
    int i;
    for ( i = 0; i < numberOfFonts; i++ ) {
        if ( fontSizes[i] == fontSize ) {
            return fonts[i];
        }
    }
    char pattern[128];
    std::snprintf(pattern, sizeof(pattern),
        "-adobe-helvetica-medium-r-normal--%d-*-*-*-*-*-iso8859-1", fontSize);
    char** missing = nullptr;
    int count = 0;
    char* fallback = nullptr;
    XFontSet set = XCreateFontSet(display, pattern, &missing, &count,
                                  &fallback);
    if ( missing ) {
        XFreeStringList(missing);
    }
    if ( !set ) {
        missing = nullptr;
        set = XCreateFontSet(display, "fixed", &missing, &count, &fallback);
        if ( missing ) {
            XFreeStringList(missing);
        }
    }
    if ( numberOfFonts == MAX_FONTS ) {
        // Full: the oldest one is replaced
        if ( fonts[0] != nullptr ) {
            XFreeFontSet(display, fonts[0]);
        }
        for ( i = 1; i < MAX_FONTS; i++ ) {
            fonts[i - 1] = fonts[i];
            fontSizes[i - 1] = fontSizes[i];
        }
        numberOfFonts--;
    }
    fonts[numberOfFonts] = set;
    fontSizes[numberOfFonts] = fontSize;
    numberOfFonts++;
    return set;
}

int XlibGizmo2DDrawListRenderer::getTextWidth(const java::String& text,
                                              int fontSize) const
{
    XFontSet set = font(fontSize);
    if ( set == nullptr ) {
        return static_cast<int>(text.length()) * fontSize / 2;
    }
    XRectangle ink;
    XRectangle logical;
    Xutf8TextExtents(set, text.c_str(), static_cast<int>(text.length()),
                     &ink, &logical);
    return logical.width;
}

int XlibGizmo2DDrawListRenderer::getTextHeight(int fontSize) const
{
    XFontSet set = font(fontSize);
    if ( set == nullptr ) {
        return fontSize;
    }
    XFontSetExtents* extents = XExtentsOfFontSet(set);
    return extents->max_logical_extent.height;
}

void XlibGizmo2DDrawListRenderer::draw(Drawable drawable, GC gc,
    Visual* visual, const Gizmo2DDrawList& list, int x0, int y0, int width,
    int height)
{
    if ( display == nullptr || list.isEmpty() ) {
        return;
    }
    java::ArrayList<Gizmo2DDrawList::Primitive> primitives =
        list.getPrimitivesInDrawingOrder();

    for ( long i = 0; i < primitives.size(); i++ ) {
        const Gizmo2DDrawList::Primitive& p = primitives[i];
        const ColorRgb& c = p.getColor();
        unsigned long rgb = static_cast<unsigned long>(clampChannel(c.r())) << 16 |
            static_cast<unsigned long>(clampChannel(c.g())) << 8 |
            static_cast<unsigned long>(clampChannel(c.b()));
        unsigned long pixel = XlibRGBImageUncompressedRenderer::rgbToPixel(
            visual, clampChannel(c.r()), clampChannel(c.g()),
            clampChannel(c.b()));

        XSetForeground(display, gc, pixel);
        switch ( p.getKind() ) {
          case Gizmo2DDrawList::Kind::POLYGON: {
            int n = p.getNumberOfVertices();
            java::ArrayList<XPoint> points;
            points.reserve(n + 1);
            for ( int v = 0; v <= n; v++ ) {
                XPoint point;
                point.x = static_cast<short>(x0 + toPixel(p.getX(v % n)));
                point.y = static_cast<short>(y0 + toPixel(p.getY(v % n)));
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
            break;
          }
          case Gizmo2DDrawList::Kind::LINE: {
            int lineWidth = toPixel(p.getWidth());
            XSetLineAttributes(display, gc, lineWidth < 1 ? 1 : lineWidth,
                               LineSolid, CapRound, JoinRound);
            XDrawLine(display, drawable, gc,
                      x0 + toPixel(p.getX(0)), y0 + toPixel(p.getY(0)),
                      x0 + toPixel(p.getX(1)), y0 + toPixel(p.getY(1)));
            break;
          }
          case Gizmo2DDrawList::Kind::LABEL: {
            XFontSet set = font(p.getFontSize());
            int textHeight = getTextHeight(p.getFontSize());
            double left = x0 + p.getX(0);
            double bottom = y0 + p.getY(0);
            const java::String& text = p.getText();

            if ( p.getAlignment() == Gizmo2DDrawList::Alignment::CENTER ) {
                left -= getTextWidth(text, p.getFontSize()) / 2.0;
                bottom += textHeight / 2.0;
            }
            if ( set != nullptr ) {
                XFontSetExtents* extents = XExtentsOfFontSet(set);
                int ascent = -extents->max_logical_extent.y;
                Xutf8DrawString(display, drawable, set, gc, toPixel(left),
                    toPixel(bottom) - textHeight + ascent, text.c_str(),
                    static_cast<int>(text.length()));
            }
            break;
          }
          default:
            break;
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
    unsigned int w = static_cast<unsigned int>(maxX - minX + 1);
    unsigned int h = static_cast<unsigned int>(maxY - minY + 1);

    //- Coverage of the polygon -------------------------------------------
    Pixmap mask = XCreatePixmap(display, drawable, w, h, 1);
    GC maskGc = XCreateGC(display, mask, 0, nullptr);
    XSetForeground(display, maskGc, 0);
    XFillRectangle(display, mask, maskGc, 0, 0, w, h);
    XSetForeground(display, maskGc, 1);
    java::ArrayList<XPoint> shifted;
    shifted.reserve(count);
    for ( i = 0; i < count; i++ ) {
        XPoint point;
        point.x = static_cast<short>(points[i].x - minX);
        point.y = static_cast<short>(points[i].y - minY);
        shifted.add(point);
    }
    XFillPolygon(display, mask, maskGc, shifted.data(), count, Complex,
                 CoordModeOrigin);
    XImage* coverage = XGetImage(display, mask, 0, 0, w, h, 1, XYPixmap);
    XFreeGC(display, maskGc);
    XFreePixmap(display, mask);
    if ( coverage == nullptr ) {
        return;
    }

    //- Blending ----------------------------------------------------------
    XImage* below = XGetImage(display, drawable, minX, minY, w, h, AllPlanes,
                              ZPixmap);
    if ( below == nullptr ) {
        XDestroyImage(coverage);
        return;
    }
    int r = static_cast<int>(rgb >> 16 & 0xFF);
    int g = static_cast<int>(rgb >> 8 & 0xFF);
    int b = static_cast<int>(rgb & 0xFF);
    for ( unsigned int row = 0; row < h; row++ ) {
        for ( unsigned int column = 0; column < w; column++ ) {
            if ( !XGetPixel(coverage, column, row) ) {
                continue;
            }
            unsigned long p = XGetPixel(below, column, row);
            int pr = channel(p, visual->red_mask);
            int pg = channel(p, visual->green_mask);
            int pb = channel(p, visual->blue_mask);
            pr = static_cast<int>(pr + (r - pr) * opacity + 0.5);
            pg = static_cast<int>(pg + (g - pg) * opacity + 0.5);
            pb = static_cast<int>(pb + (b - pb) * opacity + 0.5);
            XPutPixel(below, column, row,
                XlibRGBImageUncompressedRenderer::rgbToPixel(visual, pr, pg,
                                                             pb));
        }
    }
    XPutImage(display, drawable, gc, below, 0, 0, minX, minY, w, h);
    XDestroyImage(below);
    XDestroyImage(coverage);
}
