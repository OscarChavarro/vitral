#ifndef __GTK4_LABEL_IMAGE_PROVIDER__
#define __GTK4_LABEL_IMAGE_PROVIDER__

#include "java/lang/String.h"
#include "java/util/TreeMap.h"

#include "vsdk/toolkit/render/opengl4/OpenGL4LabelImageProvider.h"

class RGBAImageUncompressed;

/**
Rasterizes OpenGL4 labels with Pango/Cairo, using the same provider contract
as the Xlib implementation but without any X11 dependency.
*/
class Gtk4LabelImageProvider : public OpenGL4LabelImageProvider {
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

    java::TreeMap<Key, RGBAImageUncompressed*> cache;

    Gtk4LabelImageProvider(const Gtk4LabelImageProvider& other);
    Gtk4LabelImageProvider& operator=(const Gtk4LabelImageProvider& other);

public:
    Gtk4LabelImageProvider();
    virtual ~Gtk4LabelImageProvider();

    virtual RGBAImageUncompressed* createLabelImage(
        const java::String& text, const ColorRgb& color,
        int fontSize) override;
};

#endif
