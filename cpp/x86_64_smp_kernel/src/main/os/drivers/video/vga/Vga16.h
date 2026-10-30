#ifndef __VGA16__
#define __VGA16__
#include <stdint.h>

class Vga16 {
public:
    static void clear(uint8_t a = 0x07);
    static void putc(char c);
    static void set_attr(uint8_t a);

private:
    static constexpr int COLS = 80;
    static constexpr int ROWS = 25;
    static constexpr uint16_t SEG = 0xB800;
    static int cx;
    static int cy;
    static uint8_t attr;

    static void cell_write(int idx, uint16_t v);
    static uint16_t cell_read(int idx);
    static void update_cursor();
    static void scroll();
};

#endif
