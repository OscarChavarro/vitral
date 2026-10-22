#ifndef __VITRAL_RASTERIZER__
#define __VITRAL_RASTERIZER__

#include "lib/dithering/Dithering.h"
#include "lib/vgaModes/VgaModes.h"
#include "ColorMode.h"
#include "RasterizerHud.h"

class VitralRasterizer {
  public:
    static int start(int argc, char *argv[]);

  private:
    explicit VitralRasterizer(VgaModes *modes);
    VitralRasterizer(const VitralRasterizer &);
    VitralRasterizer &operator=(const VitralRasterizer &);

    int run(int mode);
    static int waitCommand();

    VgaModes *modes;
    Dithering::Palette palette;
    RasterizerHud hud;
    ColorMode::Value colorMode;
    int example;
};

#endif
