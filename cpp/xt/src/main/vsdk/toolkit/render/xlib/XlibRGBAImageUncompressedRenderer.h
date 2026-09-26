#ifndef __XLIB_RGBA_IMAGE_UNCOMPRESSED_RENDERER__
#define __XLIB_RGBA_IMAGE_UNCOMPRESSED_RENDERER__

#include <X11/Xlib.h>

class RGBAImageUncompressed;

/**
Presents vitral RGBA images with Xlib, as `AwtRGBAImageUncompressedRenderer`
does with AWT (i.e. the icons of the commands). Core X has no transparency:
the image is blended over a background color.
*/
class XlibRGBAImageUncompressedRenderer {
public:
    /**
    @param background color the transparent parts are blended with
    @return a new pixmap (free it with `XFreePixmap`), or `None` if the
    visual is not TrueColor
    */
    static Pixmap exportToPixmap(Display* display, Drawable drawable,
                                 Visual* visual, int depth,
                                 const RGBAImageUncompressed& image,
                                 const XColor& background);

private:
    XlibRGBAImageUncompressedRenderer();
};

#endif
