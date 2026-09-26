#ifndef __XLIB_LABEL_IMAGE_PROVIDER__
#define __XLIB_LABEL_IMAGE_PROVIDER__

#include <map>
#include <string>
#include <utility>

#include <X11/Xlib.h>

#include "vsdk/toolkit/render/opengl4/OpenGL4LabelImageProvider.h"

class RGBAImageUncompressed;

/**
Gives the OpenGL4 renderers the images of the labels of the viewports
(titles, HUD, gizmo labels), rasterized by `XtSystem::calculateLabelImage`,
as the `Jogl4LabelImageProvider` of `AwtJogl4ApplicationController` does
with `AwtSystem`. The images are cached, since the renderers ask for the
same labels every frame.
*/
class XlibLabelImageProvider : public OpenGL4LabelImageProvider {
private:
    typedef std::pair<std::pair<std::string, int>, unsigned long> Key;

    Display* display;
    std::map<Key, RGBAImageUncompressed*> cache;

    XlibLabelImageProvider(const XlibLabelImageProvider& other);
    XlibLabelImageProvider& operator=(const XlibLabelImageProvider& other);

public:
    explicit XlibLabelImageProvider(Display* display);
    virtual ~XlibLabelImageProvider();

    virtual RGBAImageUncompressed* createLabelImage(
        const java::String& text, const ColorRgb& color,
        int fontSize) override;
};

#endif
