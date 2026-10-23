#ifndef __XLIB_LABEL_IMAGE_PROVIDER__
#define __XLIB_LABEL_IMAGE_PROVIDER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/TreeMap.h"
#include "java/lang/String.h"

#include <X11/Xlib.h>

#include "vsdk/toolkit/render/opengl1/OpenGL1LabelImageProvider.h"
#include "application/XtSceneEditorOpenGLVariant.h"

class RGBAImageUncompressed;

/**
Key of a cached label: its text, font size and color, ordered
lexicographically.
*/
class XlibLabelImageKey {
public:
    String text;
    int fontSize;
    unsigned long rgb;

    XlibLabelImageKey() : text(""), fontSize(0), rgb(0) {}
    XlibLabelImageKey(const String& text, int fontSize, unsigned long rgb)
        : text(text), fontSize(fontSize), rgb(rgb) {}

    BOOLEAN operator<(const XlibLabelImageKey& other) const
    {
        if ( text < other.text ) return TRUE;
        if ( other.text < text ) return FALSE;
        if ( fontSize != other.fontSize ) return fontSize < other.fontSize;
        return rgb < other.rgb;
    }
};

/**
Gives the OpenGL 1.2 renderers the images of the labels of the viewports (titles, HUD, gizmo labels),
rasterized by `XtSystem::calculateLabelImage`,
as the `Jogl4LabelImageProvider` of `AwtJogl4ApplicationController` does
with `AwtSystem`. The images are cached, since the renderers ask for the
same labels every frame.
*/
class XlibLabelImageProvider : public XtOpenGLLabelImageProvider {
private:
    typedef XlibLabelImageKey Key;

    Display* display;
    TreeMap<Key, RGBAImageUncompressed*> cache;

    XlibLabelImageProvider(const XlibLabelImageProvider& other);
    XlibLabelImageProvider& operator=(const XlibLabelImageProvider& other);

public:
    explicit XlibLabelImageProvider(Display* display);
    virtual ~XlibLabelImageProvider();

    virtual RGBAImageUncompressed* createLabelImage(
        const String& text, const ColorRgb& color,
        int fontSize);
};

#endif
