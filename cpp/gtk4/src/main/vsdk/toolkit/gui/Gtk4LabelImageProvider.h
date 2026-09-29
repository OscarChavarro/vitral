#ifndef __GTK4_LABEL_IMAGE_PROVIDER__
#define __GTK4_LABEL_IMAGE_PROVIDER__

#include <map>
#include <string>
#include <utility>

#include "vsdk/toolkit/render/opengl4/OpenGL4LabelImageProvider.h"

class RGBAImageUncompressed;

/**
Rasterizes OpenGL4 labels with Pango/Cairo, using the same provider contract
as the Xlib implementation but without any X11 dependency.
*/
class Gtk4LabelImageProvider : public OpenGL4LabelImageProvider {
private:
    typedef std::pair<std::pair<std::string, int>, unsigned long> Key;

    std::map<Key, RGBAImageUncompressed*> cache;

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
