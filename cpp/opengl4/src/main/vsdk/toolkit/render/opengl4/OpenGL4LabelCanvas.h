#ifndef __OPEN_GL_4_LABEL_CANVAS__
#define __OPEN_GL_4_LABEL_CANVAS__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"

class OpenGL4LabelImageProvider;
class RGBAImageUncompressed;

/**
Transparent overlay of the size of a viewport where texts are written, as
the subset of `java.awt.Graphics2D` over a `BufferedImage` that the Java GL4
HUDs use (`setColor`, `drawString`). Each text is rasterized by an
`OpenGL4LabelImageProvider` (i.e. GTK4/Pango or Xlib) and composited over
the overlay, which is then drawn over the viewport with `draw` (as Java
draws its `BufferedImage` with `Jogl4ImageRenderer`).

The coordinates are the ones of `Graphics2D`: pixels from the upper left
corner of the viewport, with y growing downwards and the y of `drawString`
at the baseline of the text. The overlay is stored as the textures of
`OpenGL4ImageRenderer` expect (first row at the bottom).
*/
class OpenGL4LabelCanvas {
public:
    /**
    @param labels rasterizer of the texts (referenced, not owned)
    @param width width of the overlay in pixels
    @param height height of the overlay in pixels
    */
    OpenGL4LabelCanvas(OpenGL4LabelImageProvider* labels, int width,
                       int height);
    ~OpenGL4LabelCanvas();

    /**
    Resizes the overlay (if needed) and makes it fully transparent.
    */
    void clear(int width, int height);

    int getWidth() const;
    int getHeight() const;

    /**
    @param color color of the following texts
    */
    void setColor(const ColorRgb& color);

    /**
    @param fontSize size in pixels of the following texts
    */
    void setFontSize(int fontSize);

    int getFontSize() const;

    /**
    @param text text to write
    @param x left of the text
    @param y baseline of the text, from the top of the overlay
    */
    void drawString(const java::String& text, int x, int y);

    /**
    @param text a text
    @return its width in pixels with the current font size, as given by the
    label provider (Java `FontMetrics.stringWidth`)
    */
    int stringWidth(const java::String& text);

    /**
    @return the overlay image (owned by the canvas)
    */
    RGBAImageUncompressed* getImage() const;

    /**
    Draws the overlay over the whole current viewport, uploading it again
    (as Java does with `Jogl4ImageRenderer.unload` and `draw`).
    */
    void draw();

    /**
    Releases the texture of the overlay. PRE: its context is current.
    */
    void unload();

private:
    OpenGL4LabelImageProvider* labels;
    RGBAImageUncompressed* image;
    ColorRgb color;
    int fontSize;

    OpenGL4LabelCanvas(const OpenGL4LabelCanvas&);
    OpenGL4LabelCanvas& operator=(const OpenGL4LabelCanvas&);
};

#endif
