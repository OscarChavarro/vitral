#include <dos.h>
#include <graphics.h>
#include <stddef.h>
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiModes.h"
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiPointer.h"

// Code page 437 glyphs for U+00A0 to U+00FF; letters it lacks fall back to the letter without its accent.
static const unsigned char LATIN1_TO_CP437[96] = {
    0xFF, 0xAD, 0x9B, 0x9C, 0x3F, 0x9D, 0x3F, 0x3F, 0x3F, 0x3F, 0xA6, 0xAE,
    0xAA, 0x3F, 0x3F, 0x3F, 0xF8, 0xF1, 0xFD, 0x3F, 0x3F, 0xE6, 0x3F, 0xFA,
    0x3F, 0x3F, 0xA7, 0xAF, 0xAC, 0xAB, 0x3F, 0xA8, 0x41, 0x41, 0x41, 0x41,
    0x8E, 0x8F, 0x92, 0x80, 0x45, 0x90, 0x45, 0x45, 0x49, 0x49, 0x49, 0x49,
    0x3F, 0xA5, 0x4F, 0x4F, 0x4F, 0x4F, 0x99, 0x3F, 0x3F, 0x55, 0x55, 0x55,
    0x9A, 0x59, 0x3F, 0xE1, 0x85, 0xA0, 0x83, 0x61, 0x84, 0x86, 0x91, 0x87,
    0x8A, 0x82, 0x88, 0x89, 0x8D, 0xA1, 0x8C, 0x8B, 0x3F, 0xA4, 0x95, 0xA2,
    0x93, 0x6F, 0x94, 0xF6, 0x3F, 0x97, 0xA3, 0x96, 0x81, 0x79, 0x3F, 0x98,
};

// The standard 16 colours that palette modes start with.
static const unsigned long STANDARD_COLORS[16] = {
    0x000000UL, 0x0000AAUL, 0x00AA00UL, 0x00AAAAUL, 0xAA0000UL, 0xAA00AAUL, 0xAA5500UL, 0xAAAAAAUL,
    0x555555UL, 0x5555FFUL, 0x55FF55UL, 0x55FFFFUL, 0xFF5555UL, 0xFF55FFUL, 0xFFFF55UL, 0xFFFFFFUL,
};

// The numbers that the SvgaBGI 64K and TrueColor drivers take in setrgbpalette() for the colors of lines, fills and
// pixels.
enum { LINE_COLOR_SLOT = 1024, FILL_COLOR_SLOT = 1025, PIXEL_COLOR_SLOT = 1026 };

BgiFramebuffer::BgiFramebuffer()
    : screenWidth(0), screenHeight(0), depth(0), white(15), clipLeft(0), clipTop(0), clipRight(0), clipBottom(0),
      glyphRows(0), advance(8), bold(FALSE), nextEntry(0), lastEntry(-1), pointer(NULL), drawing(0), lineValue(0),
      fillValue(0), pixelValue(0), lineKnown(FALSE), fillKnown(FALSE), pixelKnown(FALSE)
{
    int i;
    for ( i = 0; i < FONT_GLYPHS * FONT_SLOT; i++ ) {
        font[i] = 0;
    }
    for ( i = 0; i < 256; i++ ) {
        palette[i] = 0;
    }
}

// One entry of the VGA DAC, in its 6 bits per component.
void
BgiFramebuffer::setDac(int entry, unsigned long color)
{
    outportb(0x3C8, (unsigned char)entry);
    outportb(0x3C9, (unsigned char)((color >> 16 & 0xFF) >> 2));
    outportb(0x3C9, (unsigned char)((color >> 8 & 0xFF) >> 2));
    outportb(0x3C9, (unsigned char)((color & 0xFF) >> 2));
}

BOOLEAN
BgiFramebuffer::attach(const BgiModes &modes)
{
    if ( !modes.inGraphics() || modes.current() < 0 ) {
        return FALSE;
    }
    depth = modes.screen().bitsPerPixel;
    if ( depth != 1 && depth != 4 && depth != 8 && depth != 16 && depth != 24 ) {
        return FALSE;
    }
    screenWidth = modes.screen().width;
    screenHeight = modes.screen().height;
    white = depth == 1 ? getmaxcolor() : 15;
    resetClip();
    int i;
    for ( i = 0; i < 256; i++ ) {
        palette[i] = i < 16 ? STANDARD_COLORS[i] : 0;
    }
    nextEntry = depth == 4 ? 1 : 16;
    lastEntry = depth == 4 ? 14 : 255;
    if ( depth == 4 || depth == 8 ) {
        // In 16 colour modes the attribute controller picks the DAC entry of each colour: made the identity, so
        // that colour i is DAC entry i.
        for ( i = 0; i < 16; i++ ) {
            if ( depth == 4 ) {
                union REGS registers;
                registers.x.ax = 0x1000;
                registers.h.bl = (unsigned char)i;
                registers.h.bh = (unsigned char)i;
                int86(0x10, &registers, &registers);
            }
            setDac(i, STANDARD_COLORS[i]);
        }
    }
    lineKnown = FALSE;
    fillKnown = FALSE;
    pixelKnown = FALSE;
    setlinestyle(SOLID_LINE, 0, NORM_WIDTH);
    setwritemode(COPY_PUT);
    loadFont();
    bold = screenWidth >= 640;
    advance = bold ? 9 : 8;
    return TRUE;
}

// The 8x16 font of the VGA BIOS (INT 10h, AX = 1130h, BH = 6), whose address comes in ES:BP.
void
BgiFramebuffer::loadFont()
{
    struct REGPACK registers;
    registers.r_ax = 0x1130;
    registers.r_bx = 0x0600;
    intr(0x10, &registers);
    const unsigned char far *bios = (const unsigned char far *)MK_FP(registers.r_es, registers.r_bp);
    int i;
    for ( i = 0; i < FONT_GLYPHS * FONT_SLOT; i++ ) {
        font[i] = bios[i];
    }
    glyphRows = 0;
    int g;
    for ( g = 0; g < FONT_GLYPHS; g++ ) {
        int row;
        for ( row = glyphRows; row < FONT_SLOT; row++ ) {
            if ( font[g * FONT_SLOT + row] ) {
                glyphRows = row + 1;
            }
        }
    }
    if ( glyphRows == 0 ) {
        glyphRows = 16;
    }
}

void
BgiFramebuffer::setPointer(BgiPointer *cursor)
{
    pointer = cursor;
}

unsigned long
BgiFramebuffer::distance(unsigned long a, unsigned long b)
{
    long dr = (long)(a >> 16 & 0xFF) - (long)(b >> 16 & 0xFF);
    long dg = (long)(a >> 8 & 0xFF) - (long)(b >> 8 & 0xFF);
    long db = (long)(a & 0xFF) - (long)(b & 0xFF);
    return (unsigned long)(dr * dr * 3 + dg * dg * 4 + db * db * 2);
}

unsigned long
BgiFramebuffer::native(unsigned long color)
{
    color &= 0xFFFFFFUL;
    unsigned long r = color >> 16;
    unsigned long g = color >> 8 & 0xFF;
    unsigned long b = color & 0xFF;
    switch ( depth ) {
    case 1:
        return r * 3 + g * 4 + b * 2 >= 128UL * 9 ? (unsigned long)white : 0;
    case 16:
        return (r >> 3) << 11 | (g >> 2) << 5 | b >> 3;
    case 24:
        return color;
    default:
        break;
    }
    // The fixed entries and those already allocated, then a new entry, then the nearest of them.
    int colors = depth == 4 ? 16 : 256;
    int i;
    for ( i = 0; i < colors; i++ ) {
        if ( usable(i) && palette[i] == color ) {
            return (unsigned long)i;
        }
    }
    if ( nextEntry <= lastEntry ) {
        int entry = nextEntry++;
        setDac(entry, color);
        palette[entry] = color;
        return (unsigned long)entry;
    }
    int best = 0;
    for ( i = 1; i < colors; i++ ) {
        if ( usable(i) && distance(palette[i], color) < distance(palette[best], color) ) {
            best = i;
        }
    }
    return (unsigned long)best;
}

// The color of the 64K and TrueColor drivers for one of the slots: 5, 6 and 5 bits, or 8 bits per component.
void
BgiFramebuffer::trueColor(int slot, unsigned long value) const
{
    if ( depth == 16 ) {
        setrgbpalette(slot, (int)(value >> 11 & 0x1F), (int)(value >> 5 & 0x3F), (int)(value & 0x1F));
    }
    else {
        setrgbpalette(slot, (int)(value >> 16 & 0xFF), (int)(value >> 8 & 0xFF), (int)(value & 0xFF));
    }
}

void
BgiFramebuffer::useLineColor(unsigned long value)
{
    if ( lineKnown && lineValue == value ) {
        return;
    }
    if ( depth >= 16 ) {
        trueColor(LINE_COLOR_SLOT, value);
    }
    setcolor((int)value);
    lineValue = value;
    lineKnown = TRUE;
}

void
BgiFramebuffer::useFillColor(unsigned long value)
{
    if ( fillKnown && fillValue == value ) {
        return;
    }
    if ( depth >= 16 ) {
        trueColor(FILL_COLOR_SLOT, value);
    }
    setfillstyle(SOLID_FILL, (int)value);
    fillValue = value;
    fillKnown = TRUE;
}

void
BgiFramebuffer::putValue(int x, int y, unsigned long value)
{
    if ( depth >= 16 && (!pixelKnown || pixelValue != value) ) {
        trueColor(PIXEL_COLOR_SLOT, value);
        pixelValue = value;
        pixelKnown = TRUE;
    }
    putpixel(x, y, (int)value);
}

void
BgiFramebuffer::setClip(int x, int y, int w, int h)
{
    clipLeft = x < 0 ? 0 : x;
    clipTop = y < 0 ? 0 : y;
    clipRight = x + w > screenWidth ? screenWidth : x + w;
    clipBottom = y + h > screenHeight ? screenHeight : y + h;
}

void
BgiFramebuffer::resetClip()
{
    setClip(0, 0, screenWidth, screenHeight);
}

void
BgiFramebuffer::beginDrawing()
{
    if ( drawing++ == 0 && pointer ) {
        pointer->hide();
    }
}

void
BgiFramebuffer::endDrawing()
{
    if ( drawing > 0 && --drawing == 0 && pointer ) {
        pointer->show();
    }
}

void
BgiFramebuffer::pixel(int x, int y, unsigned long color)
{
    if ( !inside(x, y) ) {
        return;
    }
    putValue(x, y, native(color));
}

unsigned long
BgiFramebuffer::nativePixel(int x, int y) const
{
    if ( depth == 16 ) {
        getpixel(x, y);
        return (unsigned long)(unsigned)_DX;
    }
    if ( depth == 24 ) {
        getpixel(x, y);
        unsigned low = _DX;
        // shr edx, 16
        __emit__(0x66, 0xC1, 0xEA, 0x10);
        unsigned high = _DX;
        return ((unsigned long)high << 16 | low) & 0xFFFFFFUL;
    }
    return (unsigned long)(getpixel(x, y) & 0xFF);
}

void
BgiFramebuffer::putNativePixel(int x, int y, unsigned long value)
{
    if ( x < 0 || y < 0 || x >= screenWidth || y >= screenHeight ) {
        return;
    }
    putValue(x, y, value);
}

// A clipped horizontal run of a native colour.
void
BgiFramebuffer::span(int x0, int x1, int y, unsigned long value)
{
    if ( y < clipTop || y >= clipBottom ) {
        return;
    }
    if ( x0 > x1 ) {
        int swap = x0;
        x0 = x1;
        x1 = swap;
    }
    x0 = x0 < clipLeft ? clipLeft : x0;
    x1 = x1 >= clipRight ? clipRight - 1 : x1;
    if ( x0 > x1 ) {
        return;
    }
    useLineColor(value);
    ::line(x0, y, x1, y);
}

void
BgiFramebuffer::line(int x0, int y0, int x1, int y1, unsigned long color)
{
    unsigned long value = native(color);
    if ( y0 == y1 ) {
        span(x0, x1, y0, value);
        return;
    }
    if ( x0 == x1 ) {
        if ( x0 < clipLeft || x0 >= clipRight ) {
            return;
        }
        int top = y0 < y1 ? y0 : y1;
        int bottom = y0 < y1 ? y1 : y0;
        top = top < clipTop ? clipTop : top;
        bottom = bottom >= clipBottom ? clipBottom - 1 : bottom;
        if ( top <= bottom ) {
            useLineColor(value);
            ::line(x0, top, x0, bottom);
        }
        return;
    }
    int dx = x1 >= x0 ? x1 - x0 : x0 - x1;
    int dy = y1 >= y0 ? y1 - y0 : y0 - y1;
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    long error = (long)dx - dy;
    for ( ;; ) {
        if ( inside(x0, y0) ) {
            putValue(x0, y0, value);
        }
        if ( x0 == x1 && y0 == y1 ) {
            break;
        }
        long twice = error * 2;
        if ( twice > -dy ) {
            error -= dy;
            x0 += sx;
        }
        if ( twice < dx ) {
            error += dx;
            y0 += sy;
        }
    }
}

void
BgiFramebuffer::fillRectangle(int x, int y, int w, int h, unsigned long color)
{
    if ( w <= 0 || h <= 0 ) {
        return;
    }
    int left = x < clipLeft ? clipLeft : x;
    int top = y < clipTop ? clipTop : y;
    long right = (long)x + w > clipRight ? clipRight : (long)x + w;
    long bottom = (long)y + h > clipBottom ? clipBottom : (long)y + h;
    if ( left >= right || top >= bottom ) {
        return;
    }
    useFillColor(native(color));
    bar(left, top, (int)right - 1, (int)bottom - 1);
}

void
BgiFramebuffer::rectangle(int x, int y, int w, int h, unsigned long color)
{
    if ( w <= 0 || h <= 0 ) {
        return;
    }
    line(x, y, x + w - 1, y, color);
    line(x, y + h - 1, x + w - 1, y + h - 1, color);
    line(x, y, x, y + h - 1, color);
    line(x + w - 1, y, x + w - 1, y + h - 1, color);
}

// The next code point of a UTF-8 text; malformed bytes are taken one at a time as U+FFFD.
unsigned
BgiFramebuffer::decode(const char *&text)
{
    unsigned char c = (unsigned char)*text++;
    if ( c < 0x80 ) {
        return c;
    }
    int more = c >= 0xF0 ? 3 : c >= 0xE0 ? 2 : c >= 0xC0 ? 1 : -1;
    if ( more < 0 ) {
        return 0xFFFD;
    }
    unsigned codePoint = c & (0x3F >> more);
    int i;
    for ( i = 0; i < more; i++ ) {
        if ( ((unsigned char)*text & 0xC0) != 0x80 ) {
            return 0xFFFD;
        }
        codePoint = codePoint << 6 | ((unsigned char)*text++ & 0x3F);
    }
    return codePoint;
}

unsigned char
BgiFramebuffer::glyph(unsigned codePoint)
{
    if ( codePoint >= 0x20 && codePoint < 0x7F ) {
        return (unsigned char)codePoint;
    }
    if ( codePoint >= 0xA0 && codePoint <= 0xFF ) {
        return LATIN1_TO_CP437[codePoint - 0xA0];
    }
    return '?';
}

int
BgiFramebuffer::textWidth(const char *text, int byteCount) const
{
    const char *end = text;
    while ( *end && (byteCount < 0 || end - text < byteCount) ) {
        end++;
    }
    int characters = 0;
    while ( text < end ) {
        decode(text);
        characters++;
    }
    return characters * advance;
}

// Every row of a glyph is drawn as runs of set pixels, each one a clipped horizontal line.
void
BgiFramebuffer::text(int x, int y, const char *text, unsigned long color)
{
    unsigned long value = native(color);
    while ( *text ) {
        const unsigned char *rows = font + glyph(decode(text)) * FONT_SLOT;
        int row;
        for ( row = 0; row < glyphRows; row++ ) {
            // Nine columns: bold glyphs are drawn twice, the second time one pixel to the right.
            unsigned bits = (unsigned)rows[row] << 1;
            if ( bold ) {
                bits |= bits >> 1;
            }
            int column = 0;
            while ( column < 9 ) {
                if ( !(bits & (0x100U >> column)) ) {
                    column++;
                    continue;
                }
                int start = column;
                while ( column < 9 && (bits & (0x100U >> column)) ) {
                    column++;
                }
                span(x + start, x + column - 1, y + row, value);
            }
        }
        x += advance;
    }
}
