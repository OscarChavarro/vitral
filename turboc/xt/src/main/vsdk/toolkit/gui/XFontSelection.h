#ifndef __X_FONT_SELECTION__
#define __X_FONT_SELECTION__

#include <X11/Xlib.h>

#include "vsdk/toolkit/common/VSDK.h"

/**
Font sets of the X server that are verified before use. A pattern is used
only when the server lists a font that matches it and the font set created
from it loads at least one font: Xlib may return a font set without any font
when every character set of the locale is missing, and its texts would not
be drawn. When none of the patterns is usable, the generic `fixed`, any
medium font and finally any font are tried, so a GUI keeps its texts on
servers without the requested fonts (i.e. X servers without the Adobe
fonts, or locales whose character sets the requested fonts lack).
*/
class XFontSelection {
public:
    /**
    @return true if the server has a font that matches the XLFD pattern
    */
    static BOOLEAN exists(Display* display, const char* pattern);

    /**
    @param patterns XLFD patterns in order of preference, ended by NULL
    @return the font set of the first usable pattern, else of the generic
    fallbacks; NULL only when the server has no usable font. A substitution
    is reported on the standard error.
    */
    static XFontSet createFontSet(Display* display, const char* const* patterns);

    /**
    @return the font set of the Helvetica font (bold or medium) of a pixel
    size, else of any font of that size and weight, else of the generic
    fallbacks (see createFontSet)
    */
    static XFontSet createSansFontSet(Display* display, int pixelSize, BOOLEAN bold);
};

#endif
