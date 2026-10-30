#ifndef __RAYTRACER_DISPLAY__
#define __RAYTRACER_DISPLAY__
#include <stdint.h>
#include "lib/dithering/Dithering.h"
#include "programs/vitralRaytracer/RaytracerColorMode.h"
#include "programs/vitralRaytracer/Shown.h"

class RaytracerDisplay {
public:
    static int showImage(const uint8_t *rgb, int width, int height, int text, const Dithering::Palette *standard,
                         RaytracerColorMode colorMode, Shown *shown, double *vgaSeconds);

private:
    static void drawTrueColor(const uint8_t *rgb, int width, int height);
    static void drawText(const uint8_t *rgb, int width, int height);
};

#endif
