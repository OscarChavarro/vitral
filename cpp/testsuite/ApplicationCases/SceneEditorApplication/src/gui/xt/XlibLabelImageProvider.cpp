#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/gui/XtSystem.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "gui/xt/XlibLabelImageProvider.h"

XlibLabelImageProvider::XlibLabelImageProvider(Display* display)
    : display(display)
{
}

XlibLabelImageProvider::~XlibLabelImageProvider()
{
    std::map<Key, RGBAImageUncompressed*>::iterator i;
    for ( i = cache.begin(); i != cache.end(); ++i ) {
        delete i->second;
    }
    XtSystem::releaseResources(display);
}

RGBAImageUncompressed* XlibLabelImageProvider::createLabelImage(
    const java::String& text, const ColorRgb& color, int fontSize)
{
    unsigned long rgb =
        (static_cast<unsigned long>(color.r() * 255.0) << 16) |
        (static_cast<unsigned long>(color.g() * 255.0) << 8) |
        static_cast<unsigned long>(color.b() * 255.0);
    Key key(std::make_pair(std::string(text.c_str()), fontSize), rgb);
    std::map<Key, RGBAImageUncompressed*>::iterator known = cache.find(key);
    if ( known == cache.end() ) {
        known = cache.insert(std::make_pair(key,
            XtSystem::calculateLabelImage(display, text, color, fontSize))).first;
    }
    // The renderers own the images they ask for
    const RGBAImageUncompressed* prototype = known->second;
    RGBAImageUncompressed* label = new RGBAImageUncompressed();
    // Both copy the pixels, as the Java raw image accessors
    char* pixels = prototype->getRawImage();
    label->setRawImage(prototype->getXSize(), prototype->getYSize(), pixels);
    delete[] pixels;
    return label;
}
