#include <vga.h>

#include "programs/vga/HudPatch.h"
#include "programs/vga/Hud.h"
#include "programs/vga/Picture.h"

void
HudPatch::set(const Hud *hud, const char *before, const char *after, int top, int glyphHeight, int first, int last) {
    this->hud = hud;
    this->before = before;
    this->after = after;
    left = HUD_MARGIN;
    this->top = top;
    xMin = left + first * GLYPH_WIDTH;
    xMax = left + (last + 1) * GLYPH_WIDTH;
    yMin = top;
    yMax = top + glyphHeight;
}

int
HudPatch::uncovers(int x, int y) const {
    if (x < xMin || x > xMax || y < yMin || y > yMax) {
        return 0;
    }
    return hud->pixel(before, x - left, y - top) != HUD_EMPTY && hud->pixel(after, x - left, y - top) == HUD_EMPTY;
}

void
HudPatch::draw(const Picture &picture) const {
    int color = -1;
    for (int y = yMin; y <= yMax; y++) {
        for (int x = xMin; x <= xMax; x++) {
            int was = hud->pixel(before, x - left, y - top);
            int now = hud->pixel(after, x - left, y - top);
            if (was == now) {
                continue;
            }
            int want = now == HUD_INK ? picture.getWhite() : 0;
            if (want != color) {
                vga_setcolor(want);
                color = want;
            }
            vga_drawpixel(x, y);
        }
    }
    vga_setcolor(picture.getWhite());
    picture.circle(this);
}
