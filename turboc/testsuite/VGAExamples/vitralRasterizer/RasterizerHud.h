#ifndef __RASTERIZER_HUD__
#define __RASTERIZER_HUD__

#include "vsdk/toolkit/common/VSDK.h"
#include "ColorMode.h"
#include "RasterizerShown.h"

class RasterizerHud {
  public:
    RasterizerHud();
    void loadFont();
    BOOLEAN hasFont() const;
    void toggle();
    void draw(const RasterizerShown *shown, ColorMode::Value colorMode, int width, int height, BOOLEAN text) const;

  private:
    enum {
        MARGIN = 5, LINE_GAP = 2, LINES = 2, FONT_GLYPHS = 256,
        FONT_SLOT = 32, GLYPH_WIDTH = 8
    };

    unsigned char font[FONT_GLYPHS * FONT_SLOT];
    int glyphHeight;
    int visible;

    static void selectColor(const Dithering::Palette *palette, int red, int green, int blue);
    static const char *ditheringName(const RasterizerShown *shown, ColorMode::Value colorMode);
    void drawText(int x, int y, const char *text) const;
};

#endif
