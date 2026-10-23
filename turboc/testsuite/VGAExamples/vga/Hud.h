#ifndef __HUD__
#define __HUD__

#include "vsdk/toolkit/common/VSDK.h"
#include "programs/vga/VgaConstant.h"

class Mouse;
class PaletteSet;
class Picture;

class Hud {
  public:
    Hud();

    void loadFont();
    void toggle();
    void clear();
    void draw(const Picture &picture, const PaletteSet &palettes, Mouse &mouse);
    int pixel(const char *text, int x, int y) const;
    BOOLEAN cellAt(int column, int row, char &c) const;

  private:
    unsigned char font[FONT_GLYPHS * FONT_SLOT];
    int glyphHeight;
    int visible;
    char shown[HUD_LINES][HUD_TEXT + 1];

    int glyphLit(const char *text, int length, int x, int y) const;
    void text(const Picture &picture, const PaletteSet &palettes, const Mouse &mouse,
              char lines[HUD_LINES][HUD_TEXT + 1]) const;
    void drawText(const Picture &picture, char lines[HUD_LINES][HUD_TEXT + 1]);
    static int changedCells(const char *before, const char *after, int *first, int *last);
};

#endif
