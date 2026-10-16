#ifndef __SHOWN__
#define __SHOWN__
#include "lib/dithering/Dithering.h"

class Shown {
public:
    Dithering::Palette palette;
    int dithered;
    int custom;
};

#endif
