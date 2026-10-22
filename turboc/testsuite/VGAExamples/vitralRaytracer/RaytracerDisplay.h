#ifndef __RAYTRACER_DISPLAY__
#define __RAYTRACER_DISPLAY__
#include "lib/dithering/Dithering.h"
#include "programs/vitralRaytracer/RaytracerColorMode.h"
#include "programs/vitralRaytracer/Shown.h"

class RaytracerDisplay {
public:
    static int showImage(const unsigned char *rgb, int width, int height, int text, const Dithering::Palette *standard,
                         RaytracerColorMode colorMode, Shown *shown, double *vgaSeconds);

private:
    static void drawTrueColor(const unsigned char *rgb, int width, int height);
    static void drawText(const unsigned char *rgb, int width, int height);
};

#endif
