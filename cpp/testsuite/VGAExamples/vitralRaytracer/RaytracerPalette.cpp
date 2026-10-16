#include "programs/vitralRaytracer/RaytracerPalette.h"
#include "programs/vitralRaytracer/RaytracerConstant.h"

void
RaytracerPalette::makePalette(int colors, Dithering::Palette *pal) {
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
RaytracerPalette::usesOptimizedPalette(RaytracerColorMode colorMode) {
    return colorMode == ORDERED_DITHERED_OPTIMIZED_PALETTE || colorMode == FLOYD_STEINBERG_OPTIMIZED_PALETTE;
}

Dithering::Method
RaytracerPalette::methodOf(RaytracerColorMode colorMode) {
    switch (colorMode) {
    case NEAREST: return Dithering::Method::Nearest;
    case ORDERED_DITHERED_STANDARD_PALETTE:
    case ORDERED_DITHERED_OPTIMIZED_PALETTE: return Dithering::Method::Ordered;
    default: return Dithering::Method::FloydSteinberg;
    }
}

RaytracerColorMode
RaytracerPalette::nextColorMode(RaytracerColorMode colorMode, const Dithering::Palette *standard) {
    do {
        colorMode = (RaytracerColorMode)((colorMode + 1) % COLOR_MODES);
    } while (usesOptimizedPalette(colorMode) && standard->count != 256);
    return colorMode;
}
