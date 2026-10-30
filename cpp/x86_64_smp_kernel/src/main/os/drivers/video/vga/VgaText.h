#ifndef __VGA_TEXT__
#define __VGA_TEXT__
#include <stdint.h>

class VgaText {
public:
    static constexpr int COLS = 80;
    static constexpr int ROWS = 25;
    static constexpr int MAX_ROWS = 50;

    static void init();
    static void setGeometry(int columns, int rows);

    static void putc(char c);
    static void setAttr(uint8_t a) { attr = a; }
    static uint8_t getAttr() { return attr; }
    static int cursorX() { return cx; }
    static int cursorY() { return cy; }
    static uint16_t cell(int x, int y) { return cells()[y * columns + x]; }
    static void clear(uint8_t a);

    static void putAt(int x, int y, char c, uint8_t a);
    static void writeAt(int x, int y, const char *s, uint8_t a);
    static void fillRow(int y, char c, uint8_t a);
    static void setCursor(int x, int y);
    static void showCursor(bool visible);

private:
    static constexpr uintptr_t BASE = 0xB8000;
    static int cx;
    static int cy;
    static int columns;
    static int rows;
    static uint8_t attr;

    static volatile uint16_t *cells() { return (volatile uint16_t *)BASE; }
    static void updateCursor();
    static void scroll();
};

#endif
