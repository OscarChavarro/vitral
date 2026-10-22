#ifndef __PROGRAMS_VGA__
#define __PROGRAMS_VGA__

#include "programs/vga/Hud.h"
#include "programs/vga/Menu.h"
#include "programs/vga/Mouse.h"
#include "programs/vga/PaletteSet.h"

class Vga {
  public:
    static int start(int argc, char *argv[]);

  private:
    Vga() {}
    Vga(const Vga &);
    Vga &operator=(const Vga &);

    int run(int argc, char *argv[]);
    int showPicture(int mode);

    PaletteSet palettes;
    Mouse mouse;
    Hud hud;
    Menu menu;
};

#endif
