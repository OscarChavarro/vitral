#ifndef VITRAL_SVGALIB_FRAMEBUFFER_H
#define VITRAL_SVGALIB_FRAMEBUFFER_H

#include <stdint.h>
#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"

class MouseCursor;

// Framebuffer on the current svgalib graphics mode, drawn with vga_setcolor() and vga_drawpixel() and with the VGA
// text font of vga_gettextfont(). Palette modes get exact colors in palette entries allocated on first use: indexes
// 1 to 14 in 16 colour modes (0 and 15 stay black and white) and 16 to 255 in 256 colour modes (the 16 standard
// colours stay); when the entries run out the nearest known colour is used. 2 colour modes show black and white.
class SvgalibFramebuffer : public Framebuffer {
public:
    static constexpr int FONT_GLYPHS = 256;
    static constexpr int FONT_SLOT = 32;
    static constexpr int SPAN_PIXELS = 256;

    SvgalibFramebuffer();
    // Describes the mode set by the last vga_setmode() and starts a new palette allocation. Returns false for text
    // modes and for depths other than 1, 4, 8, 16 and 24 bits per pixel.
    bool attach();
    // The value that vga_setcolor() takes for a 0xRRGGBB colour.
    uint32_t native(uint32_t color);
    // A pointer hidden while drawing, or null.
    void setPointer(MouseCursor *cursor);

    int width() const override { return screenWidth; }
    int height() const override { return screenHeight; }
    int bitsPerPixel() const override { return depth; }
    int fontWidth() const override { return advance; }
    int fontHeight() const override { return glyphRows; }
    int textWidth(const char *text, int byteCount) const override;
    void setClip(int x, int y, int width, int height) override;
    void resetClip() override;
    void pixel(int x, int y, uint32_t color) override;
    uint32_t nativePixel(int x, int y) const override;
    void putNativePixel(int x, int y, uint32_t value) override;
    void line(int x0, int y0, int x1, int y1, uint32_t color) override;
    void fillRectangle(int x, int y, int width, int height, uint32_t color) override;
    void rectangle(int x, int y, int width, int height, uint32_t color) override;
    void text(int x, int y, const char *text, uint32_t color) override;
    void beginDrawing() override;
    void endDrawing() override;

private:
    int screenWidth;
    int screenHeight;
    int depth;
    int clipLeft;
    int clipTop;
    int clipRight;
    int clipBottom;
    unsigned char font[FONT_GLYPHS * FONT_SLOT];
    int glyphRows;
    int advance;
    bool bold;
    uint32_t palette[256];
    int nextEntry;
    int lastEntry;
    MouseCursor *pointer;
    int drawing;

    bool usable(int entry) const { return entry < nextEntry || entry > lastEntry; }
    bool inside(int x, int y) const { return x >= clipLeft && y >= clipTop && x < clipRight && y < clipBottom; }
    void span(int x0, int x1, int y, uint32_t value);
    void loadFont();
    static uint32_t distance(uint32_t a, uint32_t b);
    static unsigned decode(const char *&text);
    static unsigned char glyph(unsigned codePoint);
};

#endif
