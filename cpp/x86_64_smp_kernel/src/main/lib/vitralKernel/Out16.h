#ifndef __OUT16__
#define __OUT16__
#include "drivers/video/vga/Vga16.h"

class Out16 {
public:
    static void put(char c) { Vga16::putc(c); }
};

#endif
