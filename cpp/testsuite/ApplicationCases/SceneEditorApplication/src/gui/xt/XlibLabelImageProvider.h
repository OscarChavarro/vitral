#ifndef __XLIB_LABEL_IMAGE_PROVIDER__
#define __XLIB_LABEL_IMAGE_PROVIDER__

#include <map>
#include <string>
#include <utility>
#include <vector>

#include <X11/Xlib.h>

#include "vsdk/toolkit/render/opengl4/OpenGL4LabelImageProvider.h"

/**
Rasterizes the labels of the viewports (titles, HUD, gizmo labels) with the
UTF-8 font sets of Xlib, so the OpenGL4 renderers can present them as
textures. The coverage of each text is cached, since the renderers ask for
the same labels every frame.
*/
class XlibLabelImageProvider : public OpenGL4LabelImageProvider {
private:
    struct Coverage {
        int width;
        int height;
        std::vector<unsigned char> alpha;
    };

    Display* display;
    std::map<int, XFontSet> fontSets;
    std::map<std::pair<std::string, int>, Coverage> cache;

    XFontSet getFontSet(int pixelSize);
    const Coverage& rasterize(const std::string& text, int pixelSize);

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
