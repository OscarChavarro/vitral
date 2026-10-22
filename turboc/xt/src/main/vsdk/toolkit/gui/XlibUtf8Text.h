#ifndef __XLIB_UTF8_TEXT__
#define __XLIB_UTF8_TEXT__

/**
The UTF-8 text functions of Xlib (XFree86 4 and later) or, on older X11
libraries without them (i.e. X11R6.1 on Debian 1.1), the multibyte ones of
the GUI locale, with the text in its encoding (see XtTextEncoding).
Included after <X11/Xlib.h>.
*/
#ifndef X_HAVE_UTF8_STRING
#include "java/lang/StringBuilder.h"
#include "vsdk/toolkit/gui/XtTextEncoding.h"

inline String
xlibUtf8TextInLocale(const char* text, int length)
{
    StringBuilder utf8;
    utf8.append(text, length);
    return XtTextEncoding::toLocale(utf8.toString());
}

inline int
Xutf8TextExtents(XFontSet fontSet, const char* text, int length,
                 XRectangle* ink, XRectangle* logical)
{
    String encoded = xlibUtf8TextInLocale(text, length);
    return XmbTextExtents(fontSet, encoded.c_str(), encoded.length(), ink,
                          logical);
}

inline void
Xutf8DrawString(Display* display, Drawable drawable, XFontSet fontSet,
                GC gc, int x, int y, const char* text, int length)
{
    String encoded = xlibUtf8TextInLocale(text, length);
    XmbDrawString(display, drawable, fontSet, gc, x, y, encoded.c_str(),
                  encoded.length());
}
#endif

#endif
