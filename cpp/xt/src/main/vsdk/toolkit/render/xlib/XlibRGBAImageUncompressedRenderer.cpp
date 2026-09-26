#include <X11/Xutil.h>

#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBAPixel.h"
#include "vsdk/toolkit/render/xlib/XlibRGBAImageUncompressedRenderer.h"
#include "vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.h"

Pixmap XlibRGBAImageUncompressedRenderer::exportToPixmap(
    Display* display, Drawable drawable, Visual* visual, int depth,
    const RGBAImageUncompressed& source, const XColor& background)
{
    int width = source.getXSize();
    int height = source.getYSize();
    XImage* image = XlibRGBImageUncompressedRenderer::createXImage(
        display, visual, depth, width, height);
    if ( image == nullptr ) {
        return None;
    }
    int backgroundRgb[3] = {
        background.red >> 8, background.green >> 8, background.blue >> 8
    };
    RGBAPixel pixel;
    for ( int y = 0; y < height; y++ ) {
        for ( int x = 0; x < width; x++ ) {
            source.getPixelRgba(x, y, &pixel);
            int alpha = static_cast<unsigned char>(pixel.a);
            int rgb[3] = {
                static_cast<unsigned char>(pixel.r),
                static_cast<unsigned char>(pixel.g),
                static_cast<unsigned char>(pixel.b)
            };
            for ( int c = 0; c < 3; c++ ) {
                rgb[c] = (rgb[c] * alpha + backgroundRgb[c] * (255 - alpha)) /
                    255;
            }
            XPutPixel(image, x, y, XlibRGBImageUncompressedRenderer::rgbToPixel(
                visual, rgb[0], rgb[1], rgb[2]));
        }
    }
    Pixmap pixmap = XCreatePixmap(display, drawable, width, height, depth);
    GC gc = XCreateGC(display, pixmap, 0, nullptr);
    XPutImage(display, pixmap, gc, image, 0, 0, 0, 0, width, height);
    XFreeGC(display, gc);
    XDestroyImage(image);
    return pixmap;
}
