#ifndef __RASTERIZER_DISPLAY__
#define __RASTERIZER_DISPLAY__

#include "lib/dithering/Dithering.h"
#include "ColorMode.h"
#include "RasterizerShown.h"

class RasterizerDisplay {
  public:
    static int showExample(int which, int width, int height, const Dithering::Palette *standard, ColorMode colorMode,
                           RasterizerShown *shown);

  private:
    static void drawTrueColor(const uint8_t *rgb, int width, int height);
};

#endif
