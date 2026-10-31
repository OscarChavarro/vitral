#ifndef __XLIB_LABEL_IMAGE_PROVIDER__
#define __XLIB_LABEL_IMAGE_PROVIDER__

#include "java/lang/String.h"
#include "java/util/TreeMap.h"

#include <X11/Xlib.h>

#ifdef VITRAL_SCENE_EDITOR_OPENGL1
#include "vsdk/toolkit/render/opengl1/OpenGL1LabelImageProvider.h"
#else
#include "vsdk/toolkit/render/opengl4/OpenGL4LabelImageProvider.h"
#endif
#include "application/XtSceneEditorOpenGLVariant.h"

class RGBAImageUncompressed;

/**
Gives the OpenGL renderers (4.1 or 1.2, the one the editor is built with)
the images of the labels of the viewports (titles, HUD, gizmo labels),
rasterized by `XtSystem::calculateLabelImage`,
as the `Jogl4LabelImageProvider` of `AwtJogl4ApplicationController` does
with `AwtSystem`. The images are cached, since the renderers ask for the
same labels every frame.
*/
class XlibLabelImageProvider : public XtOpenGLLabelImageProvider {
private:
    /** Key of a cached label: its text, font size and color */
    class Key {
      public:
        java::String text;
        int fontSize;
        unsigned long rgb;

        Key() : fontSize(0), rgb(0) {}
        Key(const java::String& text, int fontSize, unsigned long rgb)
            : text(text), fontSize(fontSize), rgb(rgb) {}

        bool operator<(const Key& other) const
        {
            if ( text < other.text ) {
                return true;
            }
            if ( other.text < text ) {
                return false;
            }
            if ( fontSize != other.fontSize ) {
                return fontSize < other.fontSize;
            }
            return rgb < other.rgb;
        }
    };

    Display* display;
    java::TreeMap<Key, RGBAImageUncompressed*> cache;

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
