#ifndef VITRAL_SVGALIB_FRAMEBUFFER_H
#define VITRAL_SVGALIB_FRAMEBUFFER_H

#include <stddef.h>
#include <stdint.h>
#include "lib/mouseCursor/MouseCursor.h"
#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"

// Framebuffer on the current svgalib graphics mode, with the VGA text font of vga_gettextfont(). Everything is drawn on
// a back buffer in memory, which also answers the reads of native pixels: the damaged area reaches the screen with
// vga_drawscansegment() when the outermost endDrawing() ends, as a whole, so neither a partial frame nor a read of
// video memory ever happens. In the kernel of this repository the back buffer is the console's own copy of the screen,
// so that nothing is copied twice, and the area goes out with vga_presentarea(). The mouse pointer is not part of the back buffer: it is composed over the rows that go to
// the screen, so saved areas never contain it and moving it only sends the rows it touches.
// Palette modes get exact colors in palette entries allocated on first use: indexes 1 to 14 in 16 colour modes (0 and
// 15 stay black and white) and 16 to 255 in 256 colour modes (the 16 standard colours stay); when the entries run out
// the nearest known colour is used. 2 colour modes show black and white.
class SvgalibFramebuffer : public Framebuffer {
public:
    static constexpr int FONT_GLYPHS = 256;
    static constexpr int FONT_SLOT = 32;

    SvgalibFramebuffer();
    ~SvgalibFramebuffer() override;
    SvgalibFramebuffer(const SvgalibFramebuffer &) = delete;
    SvgalibFramebuffer &operator=(const SvgalibFramebuffer &) = delete;
    // Describes the mode set by the last vga_setmode(), starts a new palette allocation and a black back buffer.
    // Returns false for text modes, for depths other than 1, 4, 8, 16 and 24 bits per pixel and without memory for the
    // buffer.
    bool attach();
    // The value that vga_setcolor() takes for a 0xRRGGBB colour.
    uint32_t native(uint32_t color);
    // The mouse pointer, with its hot spot at a surface position.
    void showPointer(int x, int y, const MouseCursor::Shape &shape = MouseCursor::ARROW);
    void movePointer(int x, int y);
    void hidePointer();
    // Sends the damaged area to the screen.
    void flush();

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
    void readNativeArea(int x, int y, int width, int height, uint32_t *values) const override;
    void writeNativeArea(int x, int y, int width, int height, const uint32_t *values) override;
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
    int drawing;
    int pixelBytes;
    size_t stride;
    unsigned char *back;
    // One row: what goes to the screen when the pointer is over it.
    unsigned char *composed;
    int damageLeft;
    int damageTop;
    int damageRight;
    int damageBottom;
    const MouseCursor::Shape *pointerShape;
    bool pointerVisible;
    int pointerLeft;
    int pointerTop;
    uint32_t pointerFill;
    uint32_t pointerOutline;

    bool usable(int entry) const { return entry < nextEntry || entry > lastEntry; }
    bool inside(int x, int y) const { return x >= clipLeft && y >= clipTop && x < clipRight && y < clipBottom; }
    unsigned char *at(int x, int y) const { return back + (size_t)y * stride + (size_t)x * (size_t)pixelBytes; }
    void store(unsigned char *target, uint32_t value) const;
    void damage(int x, int y, int width, int height);
    void sendRows(int left, int top, int right, int bottom);
    void present(int left, int top, int right, int bottom);
    void presentPointerArea(int left, int top);
    void publishPointer();
    void span(int x0, int x1, int y, uint32_t value);
    void releaseBuffers();
    void loadFont();
    static uint32_t distance(uint32_t a, uint32_t b);
    static unsigned decode(const char *&text);
    static unsigned char glyph(unsigned codePoint);
};

#endif
