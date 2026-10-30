#ifndef __MENU__
#define __MENU__

#include "lib/vgaModes/VgaModes.h"
#include "programs/vga/VgaConstant.h"

class PaletteSet;

class Menu {
  public:
    Menu();

    VgaModes &videoModes() { return modes; }
    int choose(const PaletteSet &palettes, int *mode);

  private:
    VgaModes modes;
    int selected;
    int first;
    char status[MENU_WIDTH + 1];

    void drawList();
    void draw(const PaletteSet &palettes);
    int typeMode(int *mode);
};

#endif
