#ifndef __RASTERIZER_PALETTE__
#define __RASTERIZER_PALETTE__

#include "lib/dithering/Dithering.h"
#include "ColorMode.h"

class RasterizerPalette {
  public:
    static void makePalette(int colors, Dithering::Palette *pal);
    static int usesOptimizedPalette(ColorMode colorMode);
    static Dithering::Method methodOf(ColorMode colorMode);
    static ColorMode nextColorMode(ColorMode colorMode, const Dithering::Palette *standard);

  private:
    static constexpr int CUBE_LEVELS = 6;
    static constexpr int CUBE_COLORS = CUBE_LEVELS * CUBE_LEVELS * CUBE_LEVELS;
};

#endif
