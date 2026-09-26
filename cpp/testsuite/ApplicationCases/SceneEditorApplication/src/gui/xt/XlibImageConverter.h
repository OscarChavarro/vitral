#ifndef __XLIB_IMAGE_CONVERTER__
#define __XLIB_IMAGE_CONVERTER__

#include <X11/Xlib.h>

class RGBImageUncompressed;

/**
Converts vitral images to Xlib ones, for a TrueColor visual: the images
shown by the image window and the icons of the buttons, which have a
transparency mask (as `WidgetCommand.applyTransparency` of the Java
toolkit: the opacity of a pixel is the mean of the channels of the mask).
*/
class XlibImageConverter {
public:
    /**
    @return a new image (destroy it with `XDestroyImage`), or null if the
    visual is not TrueColor
    */
    static XImage* createXImage(Display* display, Visual* visual, int depth,
                                const RGBImageUncompressed& image);

    /**
    @param mask transparency of the image (same size), or null for an
    opaque one
    @param background color the transparent parts are blended with
    @return a new pixmap (free it with `XFreePixmap`), or `None`
    */
    static Pixmap createPixmap(Display* display, Drawable drawable,
                               Visual* visual, int depth,
                               const RGBImageUncompressed& image,
                               const RGBImageUncompressed* mask,
                               const XColor& background);

private:
    XlibImageConverter();
};

#endif
