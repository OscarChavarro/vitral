#include <new>
#include <string.h>
#include <vga.h>
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibFramebuffer.h"

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
static const uint32_t EGA_COLORS[16] = {
    0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xAA5500, 0xAAAAAA,
    0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
};

SvgalibFramebuffer::SvgalibFramebuffer()
    : screenWidth(0), screenHeight(0), depth(0), clipLeft(0), clipTop(0), clipRight(0), clipBottom(0), font{},
      glyphRows(0), advance(8), bold(false), palette{}, nextEntry(0), lastEntry(-1), drawing(0), pixelBytes(1),
      stride(0), back(nullptr), composed(nullptr), damageLeft(0), damageTop(0), damageRight(0), damageBottom(0),
      pointerShape(&MouseCursor::ARROW), pointerVisible(false), pointerLeft(0), pointerTop(0), pointerFill(0),
      pointerOutline(0) {}

SvgalibFramebuffer::~SvgalibFramebuffer() {
    releaseBuffers();
}

void
SvgalibFramebuffer::releaseBuffers() {
    delete[] back;
    delete[] composed;
    back = nullptr;
    composed = nullptr;
}

bool
SvgalibFramebuffer::attach() {
    int mode = vga_getcurrentmode();
    vga_modeinfo *info = mode == TEXT ? nullptr : vga_getmodeinfo(mode);
    if (!info) {
        return false;
    }
    switch (info->colors) {
    case 2:
        depth = 1;
        break;
    case 16:
        depth = 4;
        break;
    case 256:
        depth = 8;
        break;
    case 65536:
        depth = 16;
        break;
    case 1 << 24:
        depth = info->bytesperpixel == 3 ? 24 : 0;
        break;
    default:
        depth = 0;
        break;
    }
    if (depth == 0) {
        return false;
    }
    releaseBuffers();
    screenWidth = info->width;
    screenHeight = info->height;
    pixelBytes = depth == 24 ? 3 : depth == 16 ? 2 : 1;
    stride = (size_t)screenWidth * (size_t)pixelBytes;
    back = new (std::nothrow) unsigned char[stride * (size_t)screenHeight];
    composed = new (std::nothrow) unsigned char[stride];
    if (!back || !composed) {
        releaseBuffers();
        return false;
    }
    memset(back, 0, stride * (size_t)screenHeight);
    drawing = 0;
    damageLeft = damageTop = damageRight = damageBottom = 0;
    pointerVisible = false;
    resetClip();
    for (int i = 0; i < 256; i++) {
        palette[i] = i < 16 ? EGA_COLORS[i] : 0;
    }
    nextEntry = depth == 4 ? 1 : 16;
    lastEntry = depth == 4 ? 14 : 255;
    loadFont();
    bold = screenWidth >= 640;
    advance = bold ? 9 : 8;
    return true;
}

void
SvgalibFramebuffer::loadFont() {
    vga_gettextfont(font);
    glyphRows = 0;
    for (int glyph = 0; glyph < FONT_GLYPHS; glyph++) {
        for (int row = glyphRows; row < FONT_SLOT; row++) {
            if (font[glyph * FONT_SLOT + row]) {
                glyphRows = row + 1;
            }
        }
    }
    if (glyphRows == 0) {
        glyphRows = 16;
    }
}

uint32_t
SvgalibFramebuffer::distance(uint32_t a, uint32_t b) {
    int dr = (int)(a >> 16 & 0xFF) - (int)(b >> 16 & 0xFF);
    int dg = (int)(a >> 8 & 0xFF) - (int)(b >> 8 & 0xFF);
    int db = (int)(a & 0xFF) - (int)(b & 0xFF);
    return (uint32_t)(dr * dr * 3 + dg * dg * 4 + db * db * 2);
}

uint32_t
SvgalibFramebuffer::native(uint32_t color) {
    color &= 0xFFFFFF;
    uint32_t r = color >> 16;
    uint32_t g = color >> 8 & 0xFF;
    uint32_t b = color & 0xFF;
    switch (depth) {
    case 1:
        return r * 3 + g * 4 + b * 2 >= 128 * 9 ? 15 : 0;
    case 16:
        return (r >> 3) << 11 | (g >> 2) << 5 | b >> 3;
    case 24:
        return color;
    default:
        break;
    }
    // The fixed entries and those already allocated, then a new entry, then the nearest of them.
    int colors = depth == 4 ? 16 : 256;
    for (int i = 0; i < colors; i++) {
        if (usable(i) && palette[i] == color) {
            return (uint32_t)i;
        }
    }
    if (nextEntry <= lastEntry) {
        int entry = nextEntry++;
        int rgb[3] = {(int)r >> 2, (int)g >> 2, (int)b >> 2};
        vga_setpalvec(entry, 1, rgb);
        palette[entry] = color;
        return (uint32_t)entry;
    }
    int best = 0;
    for (int i = 1; i < colors; i++) {
        if (usable(i) && distance(palette[i], color) < distance(palette[best], color)) {
            best = i;
        }
    }
    return (uint32_t)best;
}

void
SvgalibFramebuffer::setClip(int x, int y, int w, int h) {
    clipLeft = x < 0 ? 0 : x;
    clipTop = y < 0 ? 0 : y;
    clipRight = x + w > screenWidth ? screenWidth : x + w;
    clipBottom = y + h > screenHeight ? screenHeight : y + h;
}

void
SvgalibFramebuffer::resetClip() {
    setClip(0, 0, screenWidth, screenHeight);
}

void
SvgalibFramebuffer::beginDrawing() {
    drawing++;
}

void
SvgalibFramebuffer::endDrawing() {
    if (drawing > 0 && --drawing == 0) {
        flush();
    }
}

void
SvgalibFramebuffer::damage(int x, int y, int w, int h) {
    if (w <= 0 || h <= 0) {
        return;
    }
    if (damageRight <= damageLeft || damageBottom <= damageTop) {
        damageLeft = x;
        damageTop = y;
        damageRight = x + w;
        damageBottom = y + h;
        return;
    }
    damageLeft = x < damageLeft ? x : damageLeft;
    damageTop = y < damageTop ? y : damageTop;
    damageRight = x + w > damageRight ? x + w : damageRight;
    damageBottom = y + h > damageBottom ? y + h : damageBottom;
}

void
SvgalibFramebuffer::flush() {
    present(damageLeft, damageTop, damageRight, damageBottom);
    damageLeft = damageTop = damageRight = damageBottom = 0;
}

// Sends an area of the back buffer to the screen, row by row, with the pointer over the rows it touches.
void
SvgalibFramebuffer::present(int left, int top, int right, int bottom) {
    if (!back) {
        return;
    }
    left = left < 0 ? 0 : left;
    top = top < 0 ? 0 : top;
    right = right > screenWidth ? screenWidth : right;
    bottom = bottom > screenHeight ? screenHeight : bottom;
    if (left >= right || top >= bottom) {
        return;
    }
    int count = right - left;
    bool crossesPointer =
        pointerVisible && left < pointerLeft + pointerShape->width && right > pointerLeft;
    for (int y = top; y < bottom; y++) {
        unsigned char *row = at(left, y);
        if (crossesPointer && y >= pointerTop && y < pointerTop + pointerShape->height) {
            memcpy(composed, row, (size_t)count * (size_t)pixelBytes);
            const char *cells = pointerShape->rows[y - pointerTop];
            for (int column = 0; column < pointerShape->width; column++) {
                int x = pointerLeft + column;
                if (cells[column] != ' ' && x >= left && x < right) {
                    store(composed + (size_t)(x - left) * (size_t)pixelBytes,
                          cells[column] == '#' ? pointerOutline : pointerFill);
                }
            }
            row = composed;
        }
        vga_drawscansegment(row, left, y, count * pixelBytes);
    }
}

void
SvgalibFramebuffer::presentPointerArea(int left, int top) {
    present(left, top, left + pointerShape->width, top + pointerShape->height);
}

void
SvgalibFramebuffer::showPointer(int x, int y, const MouseCursor::Shape &shape) {
    pointerShape = &shape;
    pointerFill = native(0xFFFFFF);
    pointerOutline = native(0x000000);
    pointerLeft = x - shape.hotX;
    pointerTop = y - shape.hotY;
    pointerVisible = true;
    presentPointerArea(pointerLeft, pointerTop);
}

void
SvgalibFramebuffer::hidePointer() {
    if (pointerVisible) {
        pointerVisible = false;
        presentPointerArea(pointerLeft, pointerTop);
    }
}

// The rows of the old and the new position go out once, so that the pointer neither blinks nor leaves a trail.
void
SvgalibFramebuffer::movePointer(int x, int y) {
    int oldLeft = pointerLeft;
    int oldTop = pointerTop;
    pointerLeft = x - pointerShape->hotX;
    pointerTop = y - pointerShape->hotY;
    if (!pointerVisible || (oldLeft == pointerLeft && oldTop == pointerTop)) {
        return;
    }
    int w = pointerShape->width;
    int h = pointerShape->height;
    if (oldLeft < pointerLeft + w && pointerLeft < oldLeft + w && oldTop < pointerTop + h && pointerTop < oldTop + h) {
        present(oldLeft < pointerLeft ? oldLeft : pointerLeft, oldTop < pointerTop ? oldTop : pointerTop,
                (oldLeft > pointerLeft ? oldLeft : pointerLeft) + w, (oldTop > pointerTop ? oldTop : pointerTop) + h);
    } else {
        presentPointerArea(oldLeft, oldTop);
        presentPointerArea(pointerLeft, pointerTop);
    }
}

// Writes a native value at its place, least significant byte first as vga_drawscansegment() takes it.
void
SvgalibFramebuffer::store(unsigned char *target, uint32_t value) const {
    for (int b = 0; b < pixelBytes; b++) {
        target[b] = (unsigned char)(value >> (8 * b));
    }
}

void
SvgalibFramebuffer::pixel(int x, int y, uint32_t color) {
    if (!inside(x, y)) {
        return;
    }
    store(at(x, y), native(color));
    damage(x, y, 1, 1);
}

uint32_t
SvgalibFramebuffer::nativePixel(int x, int y) const {
    if (x < 0 || y < 0 || x >= screenWidth || y >= screenHeight) {
        return 0;
    }
    const unsigned char *source = at(x, y);
    uint32_t value = 0;
    for (int b = 0; b < pixelBytes; b++) {
        value |= (uint32_t)source[b] << (8 * b);
    }
    return value;
}

void
SvgalibFramebuffer::putNativePixel(int x, int y, uint32_t value) {
    if (x < 0 || y < 0 || x >= screenWidth || y >= screenHeight) {
        return;
    }
    store(at(x, y), value);
    damage(x, y, 1, 1);
}

// A clipped horizontal run of a native colour.
void
SvgalibFramebuffer::span(int x0, int x1, int y, uint32_t value) {
    if (y < clipTop || y >= clipBottom) {
        return;
    }
    if (x0 > x1) {
        int swap = x0;
        x0 = x1;
        x1 = swap;
    }
    x0 = x0 < clipLeft ? clipLeft : x0;
    x1 = x1 >= clipRight ? clipRight - 1 : x1;
    if (x0 > x1) {
        return;
    }
    unsigned char *target = at(x0, y);
    if (pixelBytes == 1) {
        memset(target, (int)value, (size_t)(x1 - x0 + 1));
    } else {
        for (int x = x0; x <= x1; x++) {
            store(target, value);
            target += pixelBytes;
        }
    }
    damage(x0, y, x1 - x0 + 1, 1);
}

void
SvgalibFramebuffer::line(int x0, int y0, int x1, int y1, uint32_t color) {
    uint32_t value = native(color);
    if (y0 == y1) {
        span(x0, x1, y0, value);
        return;
    }
    int dx = x1 >= x0 ? x1 - x0 : x0 - x1;
    int dy = y1 >= y0 ? y1 - y0 : y0 - y1;
    damage(x0 < x1 ? x0 : x1, y0 < y1 ? y0 : y1, dx + 1, dy + 1);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int error = dx - dy;
    for (;;) {
        if (inside(x0, y0)) {
            store(at(x0, y0), value);
        }
        if (x0 == x1 && y0 == y1) {
            break;
        }
        int twice = error * 2;
        if (twice > -dy) {
            error -= dy;
            x0 += sx;
        }
        if (twice < dx) {
            error += dx;
            y0 += sy;
        }
    }
}

void
SvgalibFramebuffer::fillRectangle(int x, int y, int w, int h, uint32_t color) {
    if (w <= 0 || h <= 0) {
        return;
    }
    uint32_t value = native(color);
    for (int row = y; row < y + h; row++) {
        span(x, x + w - 1, row, value);
    }
}

void
SvgalibFramebuffer::rectangle(int x, int y, int w, int h, uint32_t color) {
    if (w <= 0 || h <= 0) {
        return;
    }
    line(x, y, x + w - 1, y, color);
    line(x, y + h - 1, x + w - 1, y + h - 1, color);
    line(x, y, x, y + h - 1, color);
    line(x + w - 1, y, x + w - 1, y + h - 1, color);
}

// The next code point of a UTF-8 text; malformed bytes are taken one at a time as U+FFFD.
unsigned
SvgalibFramebuffer::decode(const char *&text) {
    unsigned char c = (unsigned char)*text++;
    if (c < 0x80) {
        return c;
    }
    int more = c >= 0xF0 ? 3 : c >= 0xE0 ? 2 : c >= 0xC0 ? 1 : -1;
    if (more < 0) {
        return 0xFFFD;
    }
    unsigned codePoint = c & (0x3F >> more);
    for (int i = 0; i < more; i++) {
        if (((unsigned char)*text & 0xC0) != 0x80) {
            return 0xFFFD;
        }
        codePoint = codePoint << 6 | ((unsigned char)*text++ & 0x3F);
    }
    return codePoint;
}

unsigned char
SvgalibFramebuffer::glyph(unsigned codePoint) {
    if (codePoint >= 0x20 && codePoint < 0x7F) {
        return (unsigned char)codePoint;
    }
    if (codePoint >= 0xA0 && codePoint <= 0xFF) {
        return LATIN1_TO_CP437[codePoint - 0xA0];
    }
    return '?';
}

int
SvgalibFramebuffer::textWidth(const char *text, int byteCount) const {
    const char *end = text;
    while (*end && (byteCount < 0 || end - text < byteCount)) {
        end++;
    }
    int characters = 0;
    while (text < end) {
        decode(text);
        characters++;
    }
    return characters * advance;
}

void
SvgalibFramebuffer::text(int x, int y, const char *text, uint32_t color) {
    uint32_t value = native(color);
    while (*text) {
        const unsigned char *rows = font + glyph(decode(text)) * FONT_SLOT;
        for (int row = 0; row < glyphRows; row++) {
            // Nine columns: bold glyphs are drawn twice, the second time one pixel to the right.
            unsigned bits = (unsigned)rows[row] << 1;
            if (bold) {
                bits |= bits >> 1;
            }
            for (int column = 0; column < 9; column++) {
                if ((bits & (0x100u >> column)) && inside(x + column, y + row)) {
                    store(at(x + column, y + row), value);
                }
            }
        }
        damage(x, y, 9, glyphRows);
        x += advance;
    }
}
