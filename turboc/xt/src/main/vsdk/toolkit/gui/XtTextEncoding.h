#ifndef __XT_TEXT_ENCODING__
#define __XT_TEXT_ENCODING__

#include "java/lang/String.h"

/**
Encoding of the texts given to X (widget labels and strings, titles, text
drawn with the X multibyte functions). The texts of the toolkit are UTF-8:
they are given to X as they are when the GUI locale is a UTF-8 one, and as
ISO 8859-1 (Latin-1) otherwise.

C++ port note: X11R6 (i.e. XFree86 3.2 on Debian 1.1) has no usable UTF-8
locale (its `en_US.utf` one does not decode UTF-8) nor ISO 10646 fonts, so
there the GUI is Latin-1. Its Xlib keeps its own locale (X_LOCALE): the
legacy Makefiles define VITRAL_X_LOCALE for it, and the locale is then
selected with `_Xsetlocale`.
*/
class XtTextEncoding {
public:
    /**
    Selects the locale of the character type of the GUI, before the font
    sets are created: `overrideLocale` (if not NULL and valid), else C.UTF-8
    or the one of the environment if they are UTF-8 locales supported by
    Xlib, else ISO 8859-1.
    @return true if the GUI locale is a UTF-8 one
    */
    static bool selectLocale(const char* overrideLocale);

    /**
    @return true if the texts are given to X in UTF-8 (the GUI locale is a
    UTF-8 one, or `selectLocale` was not called)
    */
    static bool isUtf8();

    /**
    @param text UTF-8 text
    @return the text in the encoding of the GUI locale: the same text for a
    UTF-8 locale, else ISO 8859-1 (other characters as '?')
    */
    static String toLocale(const String& text);

    /**
    @param text text in the encoding of the GUI locale (i.e. typed in a
    text field)
    @return the text in UTF-8
    */
    static String fromLocale(const String& text);

private:
    XtTextEncoding();
};

#endif
