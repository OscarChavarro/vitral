#ifndef __RAYTRACER_PALETTE__
#define __RAYTRACER_PALETTE__

#include "lib/dithering/Dithering.h"
#include "programs/vitralRaytracer/RaytracerColorMode.h"

class RaytracerPalette {
public:
    static void makePalette(int colors, Dithering::Palette *pal);
    static int usesOptimizedPalette(RaytracerColorMode colorMode);
    static Dithering::Method methodOf(RaytracerColorMode colorMode);
    static RaytracerColorMode nextColorMode(RaytracerColorMode colorMode, const Dithering::Palette *standard);
};

#endif
