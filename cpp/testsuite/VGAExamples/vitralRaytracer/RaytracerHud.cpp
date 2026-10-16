#include "programs/vitralRaytracer/RaytracerHud.h"
#include "programs/vitralRaytracer/RaytracerPalette.h"
#include <stdio.h>
#include <vga.h>
#include "vsdk/toolkit/common/VSDK.h"

RaytracerHud::RaytracerHud() : glyphHeight(0), visible(1) {}

void
RaytracerHud::loadFont() {
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
RaytracerHud::hasFont() const {
    return glyphHeight != 0;
}

void
RaytracerHud::toggle() {
    visible = !visible;
}

void
RaytracerHud::selectColor(const Dithering::Palette *palette, int red, int green, int blue) {
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

void
RaytracerHud::drawText(int x, int y, const char *text) const {
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

const char *
RaytracerHud::ditheringName(const Shown *shown, RaytracerColorMode colorMode) {
    if (!shown->dithered) {
        return "NONE";
    }
    switch (RaytracerPalette::methodOf(colorMode)) {
    case Dithering::Method::Nearest: return "STD";
    case Dithering::Method::Ordered: return "ORDERED";
    default: return "F-S";
    }
}

void
RaytracerHud::draw(const Shown *shown, RaytracerColorMode colorMode, int width, int height, int text,
                   double seconds) const {
    if (!visible) {
        return;
    }
    const char *dithering = ditheringName(shown, colorMode);
    const char *custom = shown->custom ? " CUSTOM PALETTE" : "";
    if (text) {
        printf("\033[1;1H\033[7mResolution: text %dx%d\033[2;1HRay tracing: %s s\033[3;1HDithering: %s%s\033[0m",
               TEXT_COLUMNS, TEXT_ROWS, VSDK::formatDouble(seconds, 3).c_str(), dithering, custom);
        fflush(stdout);
        return;
    }
    if (glyphHeight == 0) {
        return;
    }
    const Dithering::Palette *palette = &shown->palette;
    char lines[3][64];
    int bitsPerPixel = 0;
    while ((1L << bitsPerPixel) < (long)palette->count) {
        bitsPerPixel++;
    }
    snprintf(lines[0], sizeof(lines[0]), "Resolution: %dx%d / %dbpp", width, height, bitsPerPixel);
    snprintf(lines[1], sizeof(lines[1]), "Ray tracing: %s s", VSDK::formatDouble(seconds, 3).c_str());
    snprintf(lines[2], sizeof(lines[2]), "Dithering: %s%s", dithering, custom);
    for (int pass = 0; pass < 2; pass++) {
        int level = pass ? 255 : 0;
        selectColor(palette, level, level, level);
        for (int i = 0; i < 3; i++) {
            drawText(HUD_MARGIN + pass, HUD_MARGIN + pass + i * (glyphHeight + HUD_LINE_GAP), lines[i]);
        }
    }
}
