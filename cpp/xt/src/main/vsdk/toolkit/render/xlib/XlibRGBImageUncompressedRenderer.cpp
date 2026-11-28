#include <cstdlib>

#include <X11/Xutil.h>

#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/render/xlib/XlibColorPalette.h"
#include "vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.h"

unsigned long XlibRGBImageUncompressedRenderer::rgbToPixel(
    Display* display, const Visual* visual, int r, int g, int b)
{
    return XlibColorPalette::forVisual(display, visual)->rgbToPixel(r, g, b);
}

XImage* XlibRGBImageUncompressedRenderer::createXImage(
    Display* display, Visual* visual, int depth, int width, int height)
{
    if ( visual == nullptr || width <= 0 ||
         height <= 0 ) {
        return nullptr;
    }
    XImage* image = XCreateImage(display, visual, depth, ZPixmap, 0,
                                 nullptr, width, height, 32, 0);
    if ( image == nullptr ) {
        return nullptr;
    }
    image->data = static_cast<char*>(
        std::malloc(static_cast<size_t>(image->bytes_per_line) * height));
    if ( image->data == nullptr ) {
        XDestroyImage(image);
        return nullptr;
    }
    return image;
}

XImage* XlibRGBImageUncompressedRenderer::exportToXImage(
    Display* display, Visual* visual, int depth,
    const RGBImageUncompressed& source)
{
    int width = source.getXSize();
    int height = source.getYSize();
    XImage* image = createXImage(display, visual, depth, width, height);
    if ( image == nullptr ) {
        return nullptr;
    }
    XlibColorPalette* palette = XlibColorPalette::forVisual(display, visual);
    RGBPixel pixel;
    for ( int y = 0; y < height; y++ ) {
        for ( int x = 0; x < width; x++ ) {
            source.getPixelRgb(x, y, &pixel);
            XPutPixel(image, x, y, palette->rgbToPixel(
                static_cast<unsigned char>(pixel.r),
                static_cast<unsigned char>(pixel.g),
                static_cast<unsigned char>(pixel.b), x, y));
        }
    }
    return image;
}

void XlibRGBImageUncompressedRenderer::draw(
    Display* display, Drawable drawable, GC gc, Visual* visual, int depth,
    const RGBImageUncompressed& source, int x0, int y0)
{
    XImage* image = exportToXImage(display, visual, depth, source);
    if ( image == nullptr ) {
        return;
    }
    XPutImage(display, drawable, gc, image, 0, 0, x0, y0, image->width,
              image->height);
    XDestroyImage(image);
}
