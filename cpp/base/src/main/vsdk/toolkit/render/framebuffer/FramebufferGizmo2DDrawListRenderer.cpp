#include <cmath>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"
#include "vsdk/toolkit/render/framebuffer/FramebufferGizmo2DDrawListRenderer.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"

namespace {

/// 4x4 Bayer matrix: thresholds, from 0 to 15, of the ordered dithering
const int BAYER[4][4] = {
    { 0, 8, 2, 10 },
    { 12, 4, 14, 6 },
    { 3, 11, 1, 9 },
    { 15, 7, 13, 5 }
};

int toPixel(double value)
{
    return static_cast<int>(std::floor(value + 0.5));
}

/**
A surface that only records the horizontal spans `Framebuffer::fillPolygon`
fills (clipped to a surface size), so translucent polygons are rasterized
with the same rule as the opaque ones and then blended one span at a time,
which needs a buffer of one row only. Its edges (`line`) are left out: the
interior is enough for a translucent fill, and drawing it once per pixel
keeps the blending even.
*/
class SpanRecorder : public Framebuffer {
private:
    int surfaceWidth;
    int surfaceHeight;
    /// Spans, as (x, y, width) triples
    java::ArrayList<int> spans;

public:
    SpanRecorder(int w, int h) : surfaceWidth(w), surfaceHeight(h) {}

    int getNumberOfSpans() const { return static_cast<int>(spans.size() / 3); }
    int getSpanX(int i) const { return spans[3*i]; }
    int getSpanY(int i) const { return spans[3*i + 1]; }
    int getSpanWidth(int i) const { return spans[3*i + 2]; }

    int width() const override { return surfaceWidth; }
    int height() const override { return surfaceHeight; }
    int bitsPerPixel() const override { return 1; }
    int fontWidth() const override { return 1; }
    int fontHeight() const override { return 1; }
    int textWidth(const char*, int) const override { return 0; }
    void setClip(int, int, int, int) override {}
    void resetClip() override {}
    void pixel(int, int, uint32_t) override {}
    uint32_t nativePixel(int, int) const override { return 0; }
    void putNativePixel(int, int, uint32_t) override {}
    void line(int, int, int, int, uint32_t) override {}
    void rectangle(int, int, int, int, uint32_t) override {}
    void text(int, int, const char*, uint32_t) override {}

    void fillRectangle(int x, int y, int w, int h, uint32_t) override
    {
        int x1 = x + w;
        x = x < 0 ? 0 : x;
        x1 = x1 > surfaceWidth ? surfaceWidth : x1;
        for ( int row = y; row < y + h; row++ ) {
            if ( row >= 0 && row < surfaceHeight && x < x1 ) {
                spans.add(x);
                spans.add(row);
                spans.add(x1 - x);
            }
        }
    }
};

}

//= Text measurer =========================================================

FramebufferGizmo2DDrawListRenderer::TextMeasurer::TextMeasurer(
    const Framebuffer& s)
    : surface(s)
{
}

int FramebufferGizmo2DDrawListRenderer::TextMeasurer::getTextWidth(
    const java::String& text, int) const
{
    return surface.textWidth(text.c_str(), -1);
}

int FramebufferGizmo2DDrawListRenderer::TextMeasurer::getTextHeight(int) const
{
    return surface.fontHeight();
}

//= Drawing ===============================================================

FramebufferGizmo2DDrawListRenderer::FramebufferGizmo2DDrawListRenderer()
{
}

uint32_t FramebufferGizmo2DDrawListRenderer::packed(const ColorRgb& color)
{
    double channels[3] = {color.r(), color.g(), color.b()};
    uint32_t result = 0;

    for ( int i = 0; i < 3; i++ ) {
        double c = channels[i];
        c = c < 0 ? 0 : c > 1 ? 1 : c;
        result = result << 8 | static_cast<uint32_t>(c * 255 + 0.5);
    }
    return result;
}

void FramebufferGizmo2DDrawListRenderer::draw(Framebuffer& surface,
    const Gizmo2DDrawList& list, int x0, int y0)
{
    if ( list.isEmpty() ) {
        return;
    }
    java::ArrayList<Gizmo2DDrawList::Primitive> primitives =
        list.getPrimitivesInDrawingOrder();
    TextMeasurer measurer(surface);

    surface.beginDrawing();
    for ( long i = 0; i < primitives.size(); i++ ) {
        const Gizmo2DDrawList::Primitive& p = primitives[i];
        uint32_t color = packed(p.getColor());

        switch ( p.getKind() ) {
          case Gizmo2DDrawList::Kind::POLYGON: {
            int n = p.getNumberOfVertices();
            java::ArrayList<int> xy;
            xy.reserve(2 * n);
            for ( int v = 0; v < n; v++ ) {
                xy.add(x0 + toPixel(p.getX(v)));
                xy.add(y0 + toPixel(p.getY(v)));
            }
            if ( p.getOpacity() >= 1.0 ) {
                surface.fillPolygon(xy.data(), n, color);
            }
            else if ( p.getOpacity() > 0.0 ) {
                fillTranslucentPolygon(surface, xy.data(), n, color,
                                       p.getOpacity());
            }
            break;
          }
          case Gizmo2DDrawList::Kind::LINE: {
            double ax = x0 + p.getX(0);
            double ay = y0 + p.getY(0);
            double bx = x0 + p.getX(1);
            double by = y0 + p.getY(1);
            double dx = bx - ax;
            double dy = by - ay;
            double length = std::sqrt(dx*dx + dy*dy);
            double width = p.getWidth() > 1.0 ? p.getWidth() : 1.0;

            if ( width < 1.5 || length < 1e-6 ) {
                surface.line(toPixel(ax), toPixel(ay), toPixel(bx),
                             toPixel(by), color);
                break;
            }
            // A quad of the width of the line, around it
            double nx = -dy / length * width / 2;
            double ny = dx / length * width / 2;
            int quad[8] = {
                toPixel(ax + nx), toPixel(ay + ny),
                toPixel(bx + nx), toPixel(by + ny),
                toPixel(bx - nx), toPixel(by - ny),
                toPixel(ax - nx), toPixel(ay - ny)
            };
            surface.fillPolygon(quad, 4, color);
            break;
          }
          case Gizmo2DDrawList::Kind::LABEL: {
            int height = measurer.getTextHeight(p.getFontSize());
            double left = x0 + p.getX(0);
            double bottom = y0 + p.getY(0);

            if ( p.getAlignment() == Gizmo2DDrawList::Alignment::CENTER ) {
                left -= measurer.getTextWidth(p.getText(),
                                              p.getFontSize()) / 2.0;
                bottom += height / 2.0;
            }
            surface.text(toPixel(left), toPixel(bottom) - height,
                         p.getText().c_str(), color);
            break;
          }
          default:
            break;
        }
    }
    surface.endDrawing();
}

void FramebufferGizmo2DDrawListRenderer::fillTranslucentPolygon(
    Framebuffer& surface, const int* xy, int count, uint32_t color,
    double opacity)
{
    if ( count < 3 ) {
        return;
    }
    SpanRecorder spans(surface.width(), surface.height());
    spans.fillPolygon(xy, count, color);

    int r = (int)(color >> 16 & 0xFF);
    int g = (int)(color >> 8 & 0xFF);
    int b = (int)(color & 0xFF);
    int threshold = (int)(opacity * 16 + 0.5);
    java::ArrayList<uint32_t> row;
    bool readable = true;

    for ( int i = 0; i < spans.getNumberOfSpans(); i++ ) {
        int x = spans.getSpanX(i);
        int y = spans.getSpanY(i);
        int w = spans.getSpanWidth(i);

        //- Blending with the pixels below, when they can be read ---------
        if ( readable ) {
            while ( row.size() < w ) {
                row.add(0);
            }
            readable = surface.readImage(x, y, w, 1, row.data());
        }
        if ( readable ) {
            for ( int k = 0; k < w; k++ ) {
                uint32_t p = row[k];
                int pr = (int)(p >> 16 & 0xFF);
                int pg = (int)(p >> 8 & 0xFF);
                int pb = (int)(p & 0xFF);
                pr = (int)(pr + (r - pr) * opacity + 0.5);
                pg = (int)(pg + (g - pg) * opacity + 0.5);
                pb = (int)(pb + (b - pb) * opacity + 0.5);
                row[k] = (uint32_t)pr << 16 | (uint32_t)pg << 8 | (uint32_t)pb;
            }
            surface.putImage(x, y, w, 1, row.data());
            continue;
        }

        //- Otherwise, ordered dithering of the opacity -------------------
        for ( int k = 0; k < w; k++ ) {
            if ( BAYER[y & 3][(x + k) & 3] < threshold ) {
                surface.pixel(x + k, y, color);
            }
        }
    }
}
