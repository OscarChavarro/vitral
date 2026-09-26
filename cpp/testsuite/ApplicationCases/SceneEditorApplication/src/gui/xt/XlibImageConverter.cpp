#include <cstdlib>

#include <X11/Xutil.h>

#include "gui/xt/XlibImageConverter.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"

namespace {

int shiftOf(unsigned long mask)
{
    int shift = 0;
    while ( mask != 0 && (mask & 1) == 0 ) {
        mask >>= 1;
        shift++;
    }
    return shift;
}

unsigned long channel(int value, unsigned long mask)
{
    unsigned long bits = mask >> shiftOf(mask);
    return ((static_cast<unsigned long>(value) * bits / 255) <<
            shiftOf(mask)) & mask;
}

XImage* createEmptyImage(Display* display, Visual* visual, int depth,
                         int width, int height)
{
    if ( visual == nullptr || visual->red_mask == 0 || width <= 0 ||
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

}

XImage* XlibImageConverter::createXImage(Display* display, Visual* visual,
                                         int depth,
                                         const RGBImageUncompressed& source)
{
    int width = source.getXSize();
    int height = source.getYSize();
    XImage* image = createEmptyImage(display, visual, depth, width, height);
    if ( image == nullptr ) {
        return nullptr;
    }
    RGBPixel pixel;
    for ( int y = 0; y < height; y++ ) {
        for ( int x = 0; x < width; x++ ) {
            source.getPixelRgb(x, y, &pixel);
            XPutPixel(image, x, y,
                channel(static_cast<unsigned char>(pixel.r), visual->red_mask) |
                channel(static_cast<unsigned char>(pixel.g), visual->green_mask) |
                channel(static_cast<unsigned char>(pixel.b), visual->blue_mask));
        }
    }
    return image;
}

Pixmap XlibImageConverter::createPixmap(Display* display, Drawable drawable,
                                        Visual* visual, int depth,
                                        const RGBImageUncompressed& source,
                                        const RGBImageUncompressed* mask,
                                        const XColor& background)
{
    int width = source.getXSize();
    int height = source.getYSize();
    XImage* image = createEmptyImage(display, visual, depth, width, height);
    if ( image == nullptr ) {
        return None;
    }
    int backgroundRgb[3] = {
        background.red >> 8, background.green >> 8, background.blue >> 8
    };
    RGBPixel pixel;
    RGBPixel opacity;
    for ( int y = 0; y < height; y++ ) {
        for ( int x = 0; x < width; x++ ) {
            source.getPixelRgb(x, y, &pixel);
            int alpha = 255;
            if ( mask != nullptr && x < mask->getXSize() &&
                 y < mask->getYSize() ) {
                mask->getPixelRgb(x, y, &opacity);
                alpha = (static_cast<unsigned char>(opacity.r) +
                         static_cast<unsigned char>(opacity.g) +
                         static_cast<unsigned char>(opacity.b)) / 3;
            }
            int rgb[3] = {
                static_cast<unsigned char>(pixel.r),
                static_cast<unsigned char>(pixel.g),
                static_cast<unsigned char>(pixel.b)
            };
            for ( int c = 0; c < 3; c++ ) {
                rgb[c] = (rgb[c] * alpha + backgroundRgb[c] * (255 - alpha)) /
                    255;
            }
            XPutPixel(image, x, y,
                channel(rgb[0], visual->red_mask) |
                channel(rgb[1], visual->green_mask) |
                channel(rgb[2], visual->blue_mask));
        }
    }
    Pixmap pixmap = XCreatePixmap(display, drawable, width, height, depth);
    GC gc = XCreateGC(display, pixmap, 0, nullptr);
    XPutImage(display, pixmap, gc, image, 0, 0, 0, 0, width, height);
    XFreeGC(display, gc);
    XDestroyImage(image);
    return pixmap;
}
