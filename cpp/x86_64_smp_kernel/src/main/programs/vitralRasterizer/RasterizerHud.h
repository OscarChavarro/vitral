#ifndef __RASTERIZER_HUD__
#define __RASTERIZER_HUD__

#include "ColorMode.h"
#include "RasterizerShown.h"

class RasterizerHud {
  public:
    RasterizerHud();
    void loadFont();
    bool hasFont() const;
    void toggle();
    void draw(const RasterizerShown *shown, ColorMode colorMode, int width, int height, bool text) const;

  private:
    static constexpr int MARGIN = 5;
    static constexpr int LINE_GAP = 2;
    static constexpr int LINES = 2;
    static constexpr int FONT_GLYPHS = 256;
    static constexpr int FONT_SLOT = 32;
    static constexpr int GLYPH_WIDTH = 8;

    unsigned char font[FONT_GLYPHS * FONT_SLOT];
    int glyphHeight;
    int visible;

    static void selectColor(const Dithering::Palette *palette, int red, int green, int blue);
    static const char *ditheringName(const RasterizerShown *shown, ColorMode colorMode);
    void drawText(int x, int y, const char *text) const;
};

#endif
