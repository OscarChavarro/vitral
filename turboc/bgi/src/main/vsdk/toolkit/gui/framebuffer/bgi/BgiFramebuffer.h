#ifndef VITRAL_BGI_FRAMEBUFFER_H
#define VITRAL_BGI_FRAMEBUFFER_H

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"

class BgiModes;
class BgiPointer;

// Framebuffer on the graphics mode set by BgiModes, drawn with the Borland Graphics Interface (line(), bar() and
// putpixel()) and with the 8x16 text font of the VGA BIOS (code page 437), as SvgalibFramebuffer of the cpp port.
// Palette modes get exact colors in palette entries allocated on first use: indexes 1 to 14 in 16 colour modes (0 and
// 15 stay black and white) and 16 to 255 in 256 colour modes (the 16 standard colours stay), programmed in the VGA
// DAC; when the entries run out the nearest known colour is used. 2 colour modes show black and white. The 64K and
// TrueColor drivers of SvgaBGI take their colors through setrgbpalette() with the numbers 1024 (lines), 1025 (fills)
// and 1026 (pixels), and getpixel() leaves the whole value in DX (EDX in TrueColor).
class BgiFramebuffer : public Framebuffer {
public:
    enum { FONT_GLYPHS = 256, FONT_SLOT = 16 };

    BgiFramebuffer();
    // Describes the mode that modes shows and starts a new palette allocation. Returns false out of graphics.
    BOOLEAN attach(const BgiModes &modes);
    // The native value of a 0xRRGGBB colour.
    unsigned long native(unsigned long color);
    // A pointer hidden while drawing, or null.
    void setPointer(BgiPointer *cursor);

    virtual int width() const { return screenWidth; }
    virtual int height() const { return screenHeight; }
    virtual int bitsPerPixel() const { return depth; }
    virtual int fontWidth() const { return advance; }
    virtual int fontHeight() const { return glyphRows; }
    virtual int textWidth(const char *text, int byteCount) const;
    virtual void setClip(int x, int y, int width, int height);
    virtual void resetClip();
    virtual void pixel(int x, int y, unsigned long color);
    virtual unsigned long nativePixel(int x, int y) const;
    virtual void putNativePixel(int x, int y, unsigned long value);
    virtual void line(int x0, int y0, int x1, int y1, unsigned long color);
    virtual void fillRectangle(int x, int y, int width, int height, unsigned long color);
    virtual void rectangle(int x, int y, int width, int height, unsigned long color);
    virtual void text(int x, int y, const char *text, unsigned long color);
    virtual void beginDrawing();
    virtual void endDrawing();

private:
    int screenWidth;
    int screenHeight;
    int depth;
    int white;
    int clipLeft;
    int clipTop;
    int clipRight;
    int clipBottom;
    unsigned char font[FONT_GLYPHS * FONT_SLOT];
    int glyphRows;
    int advance;
    BOOLEAN bold;
    unsigned long palette[256];
    int nextEntry;
    int lastEntry;
    BgiPointer *pointer;
    int drawing;
    unsigned long lineValue;
    unsigned long fillValue;
    unsigned long pixelValue;
    BOOLEAN lineKnown;
    BOOLEAN fillKnown;
    BOOLEAN pixelKnown;

    BOOLEAN usable(int entry) const { return entry < nextEntry || entry > lastEntry; }
    BOOLEAN inside(int x, int y) const {
        return x >= clipLeft && y >= clipTop && x < clipRight && y < clipBottom;
    }
    void useLineColor(unsigned long value);
    void useFillColor(unsigned long value);
    void putValue(int x, int y, unsigned long value);
    void trueColor(int slot, unsigned long value) const;
    void span(int x0, int x1, int y, unsigned long value);
    void loadFont();
    static void setDac(int entry, unsigned long color);
    static unsigned long distance(unsigned long a, unsigned long b);
    static unsigned decode(const char *&text);
    static unsigned char glyph(unsigned codePoint);
};

#endif
