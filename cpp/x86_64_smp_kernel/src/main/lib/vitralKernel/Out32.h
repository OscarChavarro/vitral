#ifndef __OUT32__
#define __OUT32__
#include "drivers/video/vga/VgaText.h"

class Out32 {
public:
    static void put(char c) { VgaText::putc(c); }
};

#endif
