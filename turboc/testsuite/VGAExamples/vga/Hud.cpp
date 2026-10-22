#include <stddef.h>
#include <stdio.h>
#include <string.h>

#include <vga.h>

#include "programs/vga/Hud.h"
#include "programs/vga/HudPatch.h"
#include "programs/vga/Mouse.h"
#include "programs/vga/PaletteSet.h"
#include "programs/vga/Picture.h"

Hud::Hud() : glyphHeight(0), visible(1) {
    clear();
}

void
Hud::loadFont() {
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

void
Hud::toggle() {
    visible = !visible;
}

void
Hud::clear() {
    for (int i = 0; i < HUD_LINES; i++) {
        shown[i][0] = 0;
    }
}

int
Hud::glyphLit(const char *text, int length, int x, int y) const {
    if (x < 0 || y < 0 || y >= glyphHeight) {
        return 0;
    }
    int cell = x / GLYPH_WIDTH;
    if (cell >= length) {
        return 0;
    }
    const unsigned char *glyph = font + (unsigned char)text[cell] * FONT_SLOT;
    return glyph[y] & (0x80 >> (x % GLYPH_WIDTH));
}

int
Hud::pixel(const char *text, int x, int y) const {
    int length = (int)strlen(text);
    if (glyphLit(text, length, x - 1, y - 1)) {
        return HUD_INK;
    }
    if (glyphLit(text, length, x, y)) {
        return HUD_SHADOW;
    }
    return HUD_EMPTY;
}

void
Hud::text(const Picture &picture, const PaletteSet &palettes, const Mouse &mouse,
          char lines[HUD_LINES][HUD_TEXT + 1]) const {
    for (int i = 0; i < HUD_LINES; i++) {
        lines[i][0] = 0;
    }
    if (!visible) {
        return;
    }
    if (picture.isGraphics()) {
        snprintf(lines[0], HUD_TEXT + 1, "Resolution: %dx%d / %dbpp", picture.getWidth(), picture.getHeight(),
                 picture.getBitsPerPixel());
    } else {
        snprintf(lines[0], HUD_TEXT + 1, "Resolution: text %dx%d", picture.getWidth(), picture.getHeight());
    }
    snprintf(lines[1], HUD_TEXT + 1, "Dithering: NONE");
    if (picture.isGraphics() && picture.getColors() == 256) {
        char name[FILE_NAME];
        palettes.currentName(name, sizeof(name));
        const char *prefix = "Palette: ";
        int at = 0;
        for (; prefix[at]; at++) {
            lines[2][at] = prefix[at];
        }
        for (int i = 0; name[i] && at < HUD_TEXT; i++) {
            lines[2][at++] = name[i];
        }
        lines[2][at] = 0;
    } else {
        snprintf(lines[2], HUD_TEXT + 1, "Palette: %s", picture.getColors() > 256 ? "TRUE COLOR" : "STANDARD");
    }
    mouse.describe(lines[3], lines[4], HUD_TEXT + 1);
}

// The character a text-mode HUD line shows at a cell (drawText() pads the lines to HUD_COLUMNS), if one covers it.
bool
Hud::cellAt(int column, int row, char &c) const {
    if (row < 0 || row >= HUD_LINES || column < 0 || column >= HUD_COLUMNS || !shown[row][0]) {
        return false;
    }
    c = column < (int)strlen(shown[row]) ? shown[row][column] : ' ';
    return true;
}

void
Hud::drawText(const Picture &picture, char lines[HUD_LINES][HUD_TEXT + 1]) {
    int cleared = 0;
    for (int i = 0; i < HUD_LINES; i++) {
        if (shown[i][0] && !lines[i][0]) {
            printf("\033[%d;1H%-*s", i + 1, HUD_COLUMNS, "");
            cleared = 1;
        }
    }
    if (cleared) {
        fputs("\033[1m", stdout);
        picture.circle(NULL);
        fputs("\033[0m", stdout);
    }
    for (int i = 0; i < HUD_LINES; i++) {
        if (lines[i][0]) {
            printf("\033[%d;1H\033[7m%-*s\033[0m", i + 1, HUD_COLUMNS, lines[i]);
        }
    }
    fflush(stdout);
    memcpy(shown, lines, sizeof(shown));
}

int
Hud::changedCells(const char *before, const char *after, int *first, int *last) {
    int lengthBefore = (int)strlen(before);
    int lengthAfter = (int)strlen(after);
    int length = lengthBefore > lengthAfter ? lengthBefore : lengthAfter;
    *first = -1;
    *last = -1;
    for (int i = 0; i < length; i++) {
        char was = i < lengthBefore ? before[i] : 0;
        char now = i < lengthAfter ? after[i] : 0;
        if (was != now) {
            if (*first < 0) {
                *first = i;
            }
            *last = i;
        }
    }
    return *first >= 0;
}

void
Hud::draw(const Picture &picture, const PaletteSet &palettes, Mouse &mouse) {
    char lines[HUD_LINES][HUD_TEXT + 1];
    text(picture, palettes, mouse, lines);
    if (!picture.isGraphics()) {
        int wasShown = mouse.cursor().shown();
        mouse.cursor().hide();
        drawText(picture, lines);
        if (wasShown) {
            mouse.cursor().show();
        }
        return;
    }
    if (glyphHeight == 0) {
        return;
    }
    HudPatch patches[HUD_LINES];
    int count = 0;
    for (int i = 0; i < HUD_LINES; i++) {
        int first;
        int last;
        if (!changedCells(shown[i], lines[i], &first, &last)) {
            continue;
        }
        patches[count++].set(this, shown[i], lines[i], HUD_MARGIN + i * (glyphHeight + HUD_LINE_GAP), glyphHeight,
                             first, last);
    }
    if (count == 0) {
        return;
    }
    int x0 = patches[0].minX();
    int x1 = patches[0].maxX();
    for (int i = 1; i < count; i++) {
        x0 = patches[i].minX() < x0 ? patches[i].minX() : x0;
        x1 = patches[i].maxX() > x1 ? patches[i].maxX() : x1;
    }
    int y0 = patches[0].minY();
    int y1 = patches[count - 1].maxY();
    int covered = mouse.cursor().overlaps(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    if (covered) {
        mouse.cursor().hide();
    }
    for (int i = 0; i < count; i++) {
        patches[i].draw(picture);
    }
    if (covered) {
        mouse.cursor().show();
    }
    memcpy(shown, lines, sizeof(shown));
}
