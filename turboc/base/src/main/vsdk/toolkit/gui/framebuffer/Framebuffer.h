#ifndef VITRAL_FRAMEBUFFER_H
#define VITRAL_FRAMEBUFFER_H

#include "vsdk/toolkit/common/VSDK.h"

// A drawing surface. Coordinates are pixels with the origin at the top-left corner; rectangles use a width and height.
// Drawing is clipped to the clip rectangle, which is the whole surface until setClip() narrows it.
// Colors are packed 0xRRGGBB values. Backends map them to the graphics mode: exactly in 16 and 24 bits per pixel,
// through palette entries in 4 and 8 bits per pixel, and to black or white in 1 bit per pixel.
// Text is UTF-8 drawn with a fixed width bitmap font; characters missing from the font are shown as '?'.
// turboc port note: colors and native pixel values are `unsigned long` (32 bits on every target, where the cpp port
// uses uint32_t), as Turbo C++ 3.0 has no <stdint.h> and a 16 bit int.
class Framebuffer {
public:
    virtual ~Framebuffer() {}
    virtual int width() const = 0;
    virtual int height() const = 0;
    // 1, 4, 8, 16 or 24.
    virtual int bitsPerPixel() const = 0;
    // Horizontal advance of every character and height of a text line.
    virtual int fontWidth() const = 0;
    virtual int fontHeight() const = 0;
    // Width in pixels of the first byteCount bytes of a UTF-8 text, or of all of it when byteCount is negative.
    virtual int textWidth(const char *text, int byteCount) const = 0;
    virtual void setClip(int x, int y, int width, int height) = 0;
    virtual void resetClip() = 0;
    virtual void pixel(int x, int y, unsigned long color) = 0;
    // Native pixel values preserve palette indexes when saving and restoring an area. They ignore the clip rectangle.
    virtual unsigned long nativePixel(int x, int y) const = 0;
    virtual void putNativePixel(int x, int y, unsigned long value) = 0;
    virtual void line(int x0, int y0, int x1, int y1, unsigned long color) = 0;
    virtual void fillRectangle(int x, int y, int width, int height, unsigned long color) = 0;
    virtual void rectangle(int x, int y, int width, int height, unsigned long color) = 0;
    // Fills the polygon of count vertices (x0, y0, x1, y1, ...) with the even-odd rule, edges included. The default
    // implementation fills horizontal spans with fillRectangle(); backends with a native polygon fill override it.
    virtual void fillPolygon(const int *xy, int count, unsigned long color);
    // Draws a width x height block of 0xRRGGBB pixels, row by row from the top, with its top-left corner at (x, y).
    // The default implementation draws it with pixel(); backends that can copy whole images override it.
    virtual void putImage(int x, int y, int width, int height, const unsigned long *rgb);
    // Reads back a width x height block of the surface as 0xRRGGBB pixels. Returns false when the backend cannot
    // read its pixels as colors, which is what the default implementation does.
    virtual BOOLEAN readImage(int x, int y, int width, int height, unsigned long *rgb) const;
    // Draws text with the top-left corner of its first character cell at (x, y).
    virtual void text(int x, int y, const char *text, unsigned long color) = 0;
    // Bracket a group of drawing operations so that the backend can remove overlays, such as the mouse pointer,
    // while it draws. Calls nest.
    virtual void beginDrawing() {}
    virtual void endDrawing() {}
};

#endif
