// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Xlib.h>

#include <stdio.h>
#include "vsdk/toolkit/gui/XFontSelection.h"

// Generic fonts that every X server is expected to have, the last ones
// matching any font
static const char* const GENERIC_PATTERNS[] = {
    "fixed",
    "-*-*-medium-r-normal--*-*-*-*-*-*-*-*",
    "-*-*-*-*-*-*-*-*-*-*-*-*-*-*",
    NULL
};

BOOLEAN XFontSelection::exists(Display* display, const char* pattern)
{
    if ( display == NULL || pattern == NULL ) {
        return FALSE;
    }
    int count = 0;
    char** names = XListFonts(display, pattern, 1, &count);
    if ( names != NULL ) {
        XFreeFontNames(names);
    }
    return count > 0 ? TRUE : FALSE;
}

/**
@return the font set of a pattern, when the server has a font for it and the
font set holds at least one font; else NULL
*/
static XFontSet createVerifiedFontSet(Display* display, const char* pattern)
{
    if ( !XFontSelection::exists(display, pattern) ) {
        return NULL;
    }
    char** missingCharsets = NULL;
    int missingCharsetCount = 0;
    char* defaultString = NULL;
    XFontSet fontSet = XCreateFontSet(display, pattern, &missingCharsets,
                                      &missingCharsetCount, &defaultString);
    if ( missingCharsets != NULL ) {
        XFreeStringList(missingCharsets);
    }
    if ( fontSet == NULL ) {
        return NULL;
    }
    XFontStruct** fonts = NULL;
    char** fontNames = NULL;
    if ( XFontsOfFontSet(fontSet, &fonts, &fontNames) <= 0 ) {
        XFreeFontSet(display, fontSet);
        return NULL;
    }
    return fontSet;
}

XFontSet XFontSelection::createFontSet(Display* display, const char* const* patterns)
{
    if ( display == NULL ) {
        return NULL;
    }
    int i;
    for ( i = 0; patterns != NULL && patterns[i] != NULL; i++ ) {
        XFontSet fontSet = createVerifiedFontSet(display, patterns[i]);
        if ( fontSet != NULL ) {
            if ( i > 0 ) {
                fprintf(stderr, "Warning: X font \"%s\" is not available, "
                    "using \"%s\"\n", patterns[0], patterns[i]);
            }
            return fontSet;
        }
    }
    for ( i = 0; GENERIC_PATTERNS[i] != NULL; i++ ) {
        XFontSet fontSet = createVerifiedFontSet(display, GENERIC_PATTERNS[i]);
        if ( fontSet != NULL ) {
            fprintf(stderr, "Warning: X font \"%s\" is not available, "
                "using \"%s\"\n", patterns != NULL && patterns[0] != NULL ?
                patterns[0] : "", GENERIC_PATTERNS[i]);
            return fontSet;
        }
    }
    fprintf(stderr, "Warning: the X server has no usable font for \"%s\"\n",
        patterns != NULL && patterns[0] != NULL ? patterns[0] : "");
    return NULL;
}

XFontSet XFontSelection::createSansFontSet(Display* display, int pixelSize, BOOLEAN bold)
{
    const char* weight = bold ? "bold" : "medium";
    char helvetica[128];
    char anyHelvetica[128];
    char anyFamily[128];
    sprintf(helvetica, "-adobe-helvetica-%s-r-normal--%d-*-*-*-*-*-iso8859-1",
        weight, pixelSize);
    sprintf(anyHelvetica, "-*-helvetica-%s-r-normal--%d-*-*-*-*-*-*-*",
        weight, pixelSize);
    sprintf(anyFamily, "-*-*-%s-r-normal--%d-*-*-*-*-*-*-*", weight, pixelSize);
    const char* patterns[] = { helvetica, anyHelvetica, anyFamily, NULL };
    return createFontSet(display, patterns);
}
