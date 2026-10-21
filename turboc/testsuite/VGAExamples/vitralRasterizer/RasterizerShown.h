#ifndef __RASTERIZER_SHOWN__
#define __RASTERIZER_SHOWN__

#include "lib/dithering/Dithering.h"

class RasterizerShown {
  public:
    Dithering::Palette palette;
    int dithered;
    int custom;
};

#endif
