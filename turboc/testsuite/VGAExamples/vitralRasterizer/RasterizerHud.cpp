#include "RasterizerHud.h"
#include "RasterizerPalette.h"
#include <stdio.h>
#include <vga.h>

RasterizerHud::RasterizerHud() : glyphHeight(0), visible(1) {}

void
RasterizerHud::loadFont() {
    vga_gettextfont(font);
    glyphHeight = 0;
    for (int glyph = 0; glyph < FONT_GLYPHS; glyph++) {
        for (int row = glyphHeight; row < FONT_SLOT; row++) {
            if (font[glyph * FONT_SLOT + row]) {
                glyphHeight = row + 1;
            }
        }
    }
}

bool
RasterizerHud::hasFont() const {
    return glyphHeight != 0;
}

void
RasterizerHud::toggle() {
    visible = !visible;
}

void
RasterizerHud::selectColor(const Dithering::Palette *palette, int red, int green, int blue) {
    if (palette->count > 256) {
        vga_setrgbcolor(red, green, blue);
        return;
    }
    int best = 0;
    long bestDistance = -1;
    for (int i = 0; i < (int)palette->count; i++) {
        long dr = palette->rgb[i * 3] * 4 - red;
        long dg = palette->rgb[i * 3 + 1] * 4 - green;
        long db = palette->rgb[i * 3 + 2] * 4 - blue;
        long distance = dr * dr + dg * dg + db * db;
        if (bestDistance < 0 || distance < bestDistance) {
            best = i;
            bestDistance = distance;
        }
    }
    vga_setcolor(best);
}

const char *
RasterizerHud::ditheringName(const RasterizerShown *shown, ColorMode::Value colorMode) {
    if (!shown->dithered) {
        return "NONE";
    }
    switch (RasterizerPalette::methodOf(colorMode)) {
    case Dithering::Method::Nearest: return "STD";
    case Dithering::Method::Ordered: return "ORDERED";
    default: return "F-S";
    }
}

void
RasterizerHud::drawText(int x, int y, const char *text) const {
    for (; *text; text++, x += GLYPH_WIDTH) {
        const unsigned char *glyph = font + (unsigned char)*text * FONT_SLOT;
        for (int row = 0; row < glyphHeight; row++) {
            for (int column = 0; column < GLYPH_WIDTH; column++) {
                if (glyph[row] & (0x80 >> column)) {
                    vga_drawpixel(x + column, y + row);
                }
            }
        }
    }
}

void
RasterizerHud::draw(const RasterizerShown *shown, ColorMode::Value colorMode, int width, int height, bool text) const {
    if (!visible) {
        return;
    }
    const char *custom = shown->custom ? " CUSTOM PALETTE" : "";
    if (text) {
        printf("\033[1;1H\033[7mResolution: text %dx%d\033[2;1HDithering: %s%s\033[0m", width, height,
               ditheringName(shown, colorMode), custom);
        fflush(stdout);
        return;
    }
    if (glyphHeight == 0) {
        return;
    }
    const Dithering::Palette *palette = &shown->palette;
    char lines[LINES][64];
    int bitsPerPixel = 0;
    while ((1L << bitsPerPixel) < (long)palette->count) {
        bitsPerPixel++;
    }
    snprintf(lines[0], sizeof(lines[0]), "Resolution: %dx%d / %dbpp", width, height, bitsPerPixel);
    snprintf(lines[1], sizeof(lines[1]), "Dithering: %s%s", ditheringName(shown, colorMode), custom);
    for (int pass = 0; pass < 2; pass++) {
        int level = pass ? 255 : 0;
        selectColor(palette, level, level, level);
        for (int i = 0; i < LINES; i++) {
            drawText(MARGIN + pass, MARGIN + pass + i * (glyphHeight + LINE_GAP), lines[i]);
        }
    }
}
