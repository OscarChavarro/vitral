#include <X11/Xlib.h>

#include <cstdio>
#include "vsdk/toolkit/gui/XFontSelection.h"

// Generic fonts that every X server is expected to have, the last ones
// matching any font
static const char* const GENERIC_PATTERNS[] = {
    "fixed",
    "-*-*-medium-r-normal--*-*-*-*-*-*-*-*",
    "-*-*-*-*-*-*-*-*-*-*-*-*-*-*",
    nullptr
};

bool XFontSelection::exists(Display* display, const char* pattern)
{
    if ( display == nullptr || pattern == nullptr ) {
        return false;
    }
    int count = 0;
    char** names = XListFonts(display, pattern, 1, &count);
    if ( names != nullptr ) {
        XFreeFontNames(names);
    }
    return count > 0 ? true : false;
}

/**
@return the font set of a pattern, when the server has a font for it and the
font set holds at least one font; else nullptr
*/
static XFontSet createVerifiedFontSet(Display* display, const char* pattern)
{
    if ( !XFontSelection::exists(display, pattern) ) {
        return nullptr;
    }
    char** missingCharsets = nullptr;
    int missingCharsetCount = 0;
    char* defaultString = nullptr;
    XFontSet fontSet = XCreateFontSet(display, pattern, &missingCharsets,
                                      &missingCharsetCount, &defaultString);
    if ( missingCharsets != nullptr ) {
        XFreeStringList(missingCharsets);
    }
    if ( fontSet == nullptr ) {
        return nullptr;
    }
    XFontStruct** fonts = nullptr;
    char** fontNames = nullptr;
    if ( XFontsOfFontSet(fontSet, &fonts, &fontNames) <= 0 ) {
        XFreeFontSet(display, fontSet);
        return nullptr;
    }
    return fontSet;
}

XFontSet XFontSelection::createFontSet(Display* display, const char* const* patterns)
{
    if ( display == nullptr ) {
        return nullptr;
    }
    int i;
    for ( i = 0; patterns != nullptr && patterns[i] != nullptr; i++ ) {
        XFontSet fontSet = createVerifiedFontSet(display, patterns[i]);
        if ( fontSet != nullptr ) {
            if ( i > 0 ) {
                std::fprintf(stderr, "Warning: X font \"%s\" is not available, "
                    "using \"%s\"\n", patterns[0], patterns[i]);
            }
            return fontSet;
        }
    }
    for ( i = 0; GENERIC_PATTERNS[i] != nullptr; i++ ) {
        XFontSet fontSet = createVerifiedFontSet(display, GENERIC_PATTERNS[i]);
        if ( fontSet != nullptr ) {
            std::fprintf(stderr, "Warning: X font \"%s\" is not available, "
                "using \"%s\"\n", patterns != nullptr && patterns[0] != nullptr ?
                patterns[0] : "", GENERIC_PATTERNS[i]);
            return fontSet;
        }
    }
    std::fprintf(stderr, "Warning: the X server has no usable font for \"%s\"\n",
        patterns != nullptr && patterns[0] != nullptr ? patterns[0] : "");
    return nullptr;
}

XFontSet XFontSelection::createSansFontSet(Display* display, int pixelSize, bool bold)
{
    const char* weight = bold ? "bold" : "medium";
    char helvetica[128];
    char anyHelvetica[128];
    char anyFamily[128];
    std::snprintf(helvetica, sizeof(helvetica), "-adobe-helvetica-%s-r-normal--%d-*-*-*-*-*-iso8859-1",
        weight, pixelSize);
    std::snprintf(anyHelvetica, sizeof(anyHelvetica), "-*-helvetica-%s-r-normal--%d-*-*-*-*-*-*-*",
        weight, pixelSize);
    std::snprintf(anyFamily, sizeof(anyFamily), "-*-*-%s-r-normal--%d-*-*-*-*-*-*-*", weight, pixelSize);
    const char* patterns[] = { helvetica, anyHelvetica, anyFamily, nullptr };
    return createFontSet(display, patterns);
}
