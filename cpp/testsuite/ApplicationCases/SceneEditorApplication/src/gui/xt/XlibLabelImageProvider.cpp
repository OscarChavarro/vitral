#include <cstdio>

#include <X11/Xutil.h>

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "gui/xt/XlibLabelImageProvider.h"

XlibLabelImageProvider::XlibLabelImageProvider(Display* display)
    : display(display)
{
}

XlibLabelImageProvider::~XlibLabelImageProvider()
{
    std::map<int, XFontSet>::iterator i;
    for ( i = fontSets.begin(); i != fontSets.end(); ++i ) {
        if ( i->second != nullptr ) {
            XFreeFontSet(display, i->second);
        }
    }
}

XFontSet XlibLabelImageProvider::getFontSet(int pixelSize)
{
    std::map<int, XFontSet>::iterator known = fontSets.find(pixelSize);
    if ( known != fontSets.end() ) {
        return known->second;
    }

    // Closest size of a scalable or bitmap Helvetica, then any font
    char pattern[256];
    snprintf(pattern, sizeof(pattern),
        "-*-helvetica-medium-r-normal--%d-*-*-*-*-*-*-*,"
        "-*-*-medium-r-normal--%d-*-*-*-*-*-*-*,fixed",
        pixelSize, pixelSize);
    char** missingCharsets = nullptr;
    int missingCharsetCount = 0;
    char* defaultString = nullptr;
    XFontSet fontSet = XCreateFontSet(display, pattern, &missingCharsets,
                                      &missingCharsetCount, &defaultString);
    if ( missingCharsets != nullptr ) {
        XFreeStringList(missingCharsets);
    }
    fontSets[pixelSize] = fontSet;
    return fontSet;
}

const XlibLabelImageProvider::Coverage& XlibLabelImageProvider::rasterize(
    const std::string& text, int pixelSize)
{
    std::pair<std::string, int> key(text, pixelSize);
    std::map<std::pair<std::string, int>, Coverage>::iterator known =
        cache.find(key);
    if ( known != cache.end() ) {
        return known->second;
    }

    Coverage& coverage = cache[key];
    coverage.width = 0;
    coverage.height = 0;
    XFontSet fontSet = getFontSet(pixelSize);
    if ( fontSet == nullptr || text.empty() ) {
        return coverage;
    }

    XRectangle ink;
    XRectangle logical;
    Xutf8TextExtents(fontSet, text.c_str(), static_cast<int>(text.size()),
                     &ink, &logical);
    int width = logical.width > 0 ? logical.width : 1;
    int height = logical.height > 0 ? logical.height : 1;

    // White text over black: the red channel is the coverage of the text
    int screen = DefaultScreen(display);
    Window root = RootWindow(display, screen);
    Pixmap pixmap = XCreatePixmap(display, root, width, height,
                                  DefaultDepth(display, screen));
    GC gc = XCreateGC(display, pixmap, 0, nullptr);
    XSetForeground(display, gc, BlackPixel(display, screen));
    XFillRectangle(display, pixmap, gc, 0, 0, width, height);
    XSetForeground(display, gc, WhitePixel(display, screen));
    Xutf8DrawString(display, pixmap, fontSet, gc, -logical.x, -logical.y,
                    text.c_str(), static_cast<int>(text.size()));
    XImage* image = XGetImage(display, pixmap, 0, 0, width, height,
                              AllPlanes, ZPixmap);
    if ( image != nullptr ) {
        coverage.width = width;
        coverage.height = height;
        coverage.alpha.resize(static_cast<size_t>(width) * height);
        unsigned long white = WhitePixel(display, screen);
        for ( int y = 0; y < height; y++ ) {
            for ( int x = 0; x < width; x++ ) {
                coverage.alpha[static_cast<size_t>(y) * width + x] =
                    XGetPixel(image, x, y) == white ? 255 : 0;
            }
        }
        XDestroyImage(image);
    }
    XFreeGC(display, gc);
    XFreePixmap(display, pixmap);
    return coverage;
}

RGBAImageUncompressed* XlibLabelImageProvider::createLabelImage(
    const java::String& text, const ColorRgb& color, int fontSize)
{
    const Coverage& coverage = rasterize(text.c_str(), fontSize);
    RGBAImageUncompressed* label = new RGBAImageUncompressed();

    // Renderers use the image without checking it: an empty text (or a
    // missing font) gives a transparent pixel
    if ( coverage.width <= 0 || coverage.height <= 0 ) {
        label->init(1, 1);
        return label;
    }
    label->init(coverage.width, coverage.height);
    char r = static_cast<char>(static_cast<int>(color.r() * 255.0));
    char g = static_cast<char>(static_cast<int>(color.g() * 255.0));
    char b = static_cast<char>(static_cast<int>(color.b() * 255.0));
    for ( int y = 0; y < coverage.height; y++ ) {
        for ( int x = 0; x < coverage.width; x++ ) {
            unsigned char a =
                coverage.alpha[static_cast<size_t>(y) * coverage.width + x];
            // putPixel takes y from the top, as the X image does
            label->putPixel(x, y, r, g, b, static_cast<char>(a));
        }
    }
    return label;
}
