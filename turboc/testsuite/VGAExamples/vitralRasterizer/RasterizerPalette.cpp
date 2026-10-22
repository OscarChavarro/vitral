#include "RasterizerPalette.h"

void
RasterizerPalette::makePalette(int colors, Dithering::Palette *pal) {
    pal->count = colors;
    if (colors > 256) {
      return;
    }
    for (int i = 0; i < colors; i++) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (colors == 256) {
            if (i < CUBE_COLORS) {
                r = i / (CUBE_LEVELS * CUBE_LEVELS) * 63 / (CUBE_LEVELS - 1);
                g = i / CUBE_LEVELS % CUBE_LEVELS * 63 / (CUBE_LEVELS - 1);
                b = i % CUBE_LEVELS * 63 / (CUBE_LEVELS - 1);
            } else {
                r = g = b = (i - CUBE_COLORS) * 63 / (256 - CUBE_COLORS - 1);
            }
        } else if (colors == 16) {
            r = i & 4 ? 42 : 0;
            g = i & 2 ? 42 : 0;
            b = i & 1 ? 42 : 0;
            if (i == 6) {
                g = 21;
            }
            if (i >= 8) {
                r = r ? 63 : 21;
                g = g ? 63 : 21;
                b = b ? 63 : 21;
            }
        } else {
            r = g = b = i ? 63 : 0;
        }
        pal->rgb[i * 3] = r;
        pal->rgb[i * 3 + 1] = g;
        pal->rgb[i * 3 + 2] = b;
    }
}

int
RasterizerPalette::usesOptimizedPalette(ColorMode::Value colorMode) {
    return colorMode == ColorMode::ORDERED_DITHERED_OPTIMIZED_PALETTE || colorMode == ColorMode::FLOYD_STEINBERG_OPTIMIZED_PALETTE;
}

Dithering::Method
RasterizerPalette::methodOf(ColorMode::Value colorMode) {
    switch (colorMode) {
    case ColorMode::NEAREST: return Dithering::Method::Nearest;
    case ColorMode::ORDERED_DITHERED_STANDARD_PALETTE:
    case ColorMode::ORDERED_DITHERED_OPTIMIZED_PALETTE: return Dithering::Method::Ordered;
    default: return Dithering::Method::FloydSteinberg;
    }
}

ColorMode::Value
RasterizerPalette::nextColorMode(ColorMode::Value colorMode, const Dithering::Palette *standard) {
    do {
        colorMode = ((ColorMode::Value)((((int)(colorMode)) + 1) % ((int)(ColorMode::COLOR_MODES))));
    } while (usesOptimizedPalette(colorMode) && standard->count != 256);
    return colorMode;
}

