#include <algorithm>
#include <cstdint>
#include <cstring>

#include <cairo.h>
#include <pango/pangocairo.h>

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/gui/Gtk4LabelImageProvider.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"

Gtk4LabelImageProvider::Gtk4LabelImageProvider()
{
}

Gtk4LabelImageProvider::~Gtk4LabelImageProvider()
{
    std::map<Key, RGBAImageUncompressed*>::iterator i;
    for ( i = cache.begin(); i != cache.end(); ++i ) {
        delete i->second;
    }
}

RGBAImageUncompressed* Gtk4LabelImageProvider::createLabelImage(
    const java::String& text, const ColorRgb& color, int fontSize)
{
    unsigned long rgb =
        (static_cast<unsigned long>(color.r() * 255.0) << 16) |
        (static_cast<unsigned long>(color.g() * 255.0) << 8) |
        static_cast<unsigned long>(color.b() * 255.0);
    Key key(std::make_pair(std::string(text.c_str()), fontSize), rgb);
    std::map<Key, RGBAImageUncompressed*>::iterator known = cache.find(key);
    if ( known == cache.end() ) {
        PangoFontDescription* font = pango_font_description_new();
        pango_font_description_set_family(font, "Sans");
        pango_font_description_set_absolute_size(
            font, std::max(1, fontSize) * PANGO_SCALE);

        cairo_surface_t* probeSurface =
            cairo_image_surface_create(CAIRO_FORMAT_ARGB32, 1, 1);
        cairo_t* probe = cairo_create(probeSurface);
        PangoLayout* layout = pango_cairo_create_layout(probe);
        pango_layout_set_font_description(layout, font);
        pango_layout_set_text(layout, text.c_str(), -1);

        int textWidth = 0;
        int textHeight = 0;
        pango_layout_get_pixel_size(layout, &textWidth, &textHeight);
        const int width = std::max(1, textWidth + 4);
        const int height = std::max(1, textHeight + 4);

        g_object_unref(layout);
        cairo_destroy(probe);
        cairo_surface_destroy(probeSurface);

        cairo_surface_t* surface =
            cairo_image_surface_create(CAIRO_FORMAT_ARGB32, width, height);
        cairo_t* cr = cairo_create(surface);
        cairo_set_operator(cr, CAIRO_OPERATOR_CLEAR);
        cairo_paint(cr);
        cairo_set_operator(cr, CAIRO_OPERATOR_OVER);
        cairo_set_source_rgba(cr, color.r(), color.g(), color.b(), 1.0);
        layout = pango_cairo_create_layout(cr);
        pango_layout_set_font_description(layout, font);
        pango_layout_set_text(layout, text.c_str(), -1);
        cairo_move_to(cr, 2.0, 2.0);
        pango_cairo_show_layout(cr, layout);
        cairo_surface_flush(surface);

        unsigned char* source = cairo_image_surface_get_data(surface);
        const int stride = cairo_image_surface_get_stride(surface);
        char* rgba = new char[width * height * 4];
        for ( int y = 0; y < height; y++ ) {
            const uint32_t* row =
                reinterpret_cast<const uint32_t*>(source + y * stride);
            for ( int x = 0; x < width; x++ ) {
                uint32_t p = row[x];
                unsigned char a = static_cast<unsigned char>((p >> 24) & 0xff);
                const int out = ((height - 1 - y) * width + x) * 4;
                rgba[out + 0] = static_cast<char>(color.r() * 255.0);
                rgba[out + 1] = static_cast<char>(color.g() * 255.0);
                rgba[out + 2] = static_cast<char>(color.b() * 255.0);
                rgba[out + 3] = static_cast<char>(a);
            }
        }

        RGBAImageUncompressed* image = new RGBAImageUncompressed();
        image->setRawImage(width, height, rgba);
        delete[] rgba;

        g_object_unref(layout);
        cairo_destroy(cr);
        cairo_surface_destroy(surface);
        pango_font_description_free(font);

        known = cache.insert(std::make_pair(key, image)).first;
    }

    const RGBAImageUncompressed* prototype = known->second;
    RGBAImageUncompressed* label = new RGBAImageUncompressed();
    char* pixels = prototype->getRawImage();
    label->setRawImage(prototype->getXSize(), prototype->getYSize(), pixels);
    delete[] pixels;
    return label;
}
