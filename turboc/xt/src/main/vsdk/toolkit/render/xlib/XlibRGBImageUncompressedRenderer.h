#ifndef __XLIB_RGB_IMAGE_UNCOMPRESSED_RENDERER__
#define __XLIB_RGB_IMAGE_UNCOMPRESSED_RENDERER__

#include <X11/Xlib.h>

class RGBImageUncompressed;

/**
Presents vitral RGB images with Xlib, as `AwtRGBImageUncompressedRenderer`
does with AWT: drawing them over a drawable and exporting them to Xlib
images, for TrueColor visuals.
*/
class XlibRGBImageUncompressedRenderer {
public:
    /**
    Draws the image with its upper left corner at a position.
    */
    static void draw(Display* display, Drawable drawable, GC gc,
                     Visual* visual, int depth,
                     const RGBImageUncompressed& image, int x0, int y0);

    /**
    @return a new image (destroy it with `XDestroyImage`), or null if the
    visual is not TrueColor
    */
    static XImage* exportToXImage(Display* display, Visual* visual,
                                  int depth,
                                  const RGBImageUncompressed& image);

    /**
    @return an empty image of a TrueColor visual (destroy it with
    `XDestroyImage`), or null
    */
    static XImage* createXImage(Display* display, Visual* visual, int depth,
                                int width, int height);

    /**
    @param r red, in [0, 255]
    @param g green, in [0, 255]
    @param b blue, in [0, 255]
    @return the pixel value of the color in a TrueColor visual
    */
    static unsigned long rgbToPixel(const Visual* visual, int r, int g, int b);

private:
    XlibRGBImageUncompressedRenderer();
};

#endif
