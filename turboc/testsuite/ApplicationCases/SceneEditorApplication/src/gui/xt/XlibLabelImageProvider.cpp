#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/gui/XtSystem.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "gui/xt/XlibLabelImageProvider.h"
#include "java/util/TreeMap.h"

XlibLabelImageProvider::XlibLabelImageProvider(Display* display)
    : display(display)
{
}

XlibLabelImageProvider::~XlibLabelImageProvider()
{
    for ( long i = 0; i < cache.size(); i++ ) {
        delete cache.getValue(i);
    }
    XtSystem::releaseResources(display);
}

RGBAImageUncompressed* XlibLabelImageProvider::createLabelImage(
    const String& text, const ColorRgb& color, int fontSize)
{
    unsigned long rgb =
        (((unsigned long)(color.r() * 255.0)) << 16) |
        (((unsigned long)(color.g() * 255.0)) << 8) |
        ((unsigned long)(color.b() * 255.0));
    Key key(text, fontSize, rgb);
    RGBAImageUncompressed** known = cache.get(key);
    if ( known == NULL ) {
        known = &cache.putIfAbsent(key,
            XtSystem::calculateLabelImage(display, text, color, fontSize));
    }
    // The renderers own the images they ask for
    const RGBAImageUncompressed* prototype = (*known);
    RGBAImageUncompressed* label = new RGBAImageUncompressed();
    // Both copy the pixels, as the Java raw image accessors
    char* pixels = prototype->getRawImage();
    label->setRawImage(prototype->getXSize(), prototype->getYSize(), pixels);
    delete[] pixels;
    return label;
}
