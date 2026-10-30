#include "drivers/video/vga/VgaText.h"
#include "lib/vitralKernel/Ports.h"

int VgaText::cx = 0;
int VgaText::cy = 0;
int VgaText::columns = COLS;
int VgaText::rows = ROWS;
uint8_t VgaText::attr = 0x07;

void
VgaText::updateCursor() {
    uint16_t pos = (uint16_t)(cy * columns + cx);
    Ports::outb(0x3D4, 0x0F); Ports::outb(0x3D5, (uint8_t)pos);
    Ports::outb(0x3D4, 0x0E); Ports::outb(0x3D5, (uint8_t)(pos >> 8));
}

void
VgaText::scroll() {
    volatile uint16_t *vga = cells();
    for (int i = 0; i < (rows - 1) * columns; i++) {
        vga[i] = vga[i + columns];
    }
    for (int i = (rows - 1) * columns; i < rows * columns; i++) {
        vga[i] = (uint16_t)(attr << 8) | ' ';
    }
    cy = rows - 1;
}

void
VgaText::init() {
    Ports::outb(0x3D4, 0x0F);
    uint16_t pos = Ports::inb(0x3D5);
    Ports::outb(0x3D4, 0x0E);
    pos |= (uint16_t)Ports::inb(0x3D5) << 8;
    cx = pos % columns;
    cy = pos / columns;
}

void
VgaText::setGeometry(int newColumns, int newRows) {
    columns = newColumns;
    rows = newRows;
    if (cx >= columns) {
        cx = columns - 1;
    }
    if (cy >= rows) {
        cy = rows - 1;
    }
}

void
VgaText::putc(char c) {
    if (c == '\n') {
        cx = 0;
        cy++;
    } else if (c == '\r') {
        cx = 0;
    } else if (c == '\t') {
        cx = (cx + 8) & ~7;
    } else {
        cells()[cy * columns + cx] = (uint16_t)(attr << 8) | (uint8_t)c;
        cx++;
    }
    if (cx >= columns) {
        cx = 0;
        cy++;
    }
    if (cy >= rows) {
        scroll();
    }
    updateCursor();
}

void
VgaText::clear(uint8_t a) {
    attr = a;
    for (int i = 0; i < rows * columns; i++) {
        cells()[i] = (uint16_t)(a << 8) | ' ';
    }
    cx = cy = 0;
    updateCursor();
}

void
VgaText::putAt(int x, int y, char c, uint8_t a) {
    if (x < 0 || x >= columns || y < 0 || y >= rows) {
        return;
    }
    cells()[y * columns + x] = (uint16_t)(a << 8) | (uint8_t)c;
}

void
VgaText::writeAt(int x, int y, const char *s, uint8_t a) {
    for (; *s && x < columns; s++, x++) {
        putAt(x, y, *s, a);
    }
}

void
VgaText::fillRow(int y, char c, uint8_t a) {
    for (int x = 0; x < columns; x++) {
        putAt(x, y, c, a);
    }
}

void
VgaText::setCursor(int x, int y) {
    cx = x;
    cy = y;
    updateCursor();
}

void
VgaText::showCursor(bool visible) {
    Ports::outb(0x3D4, 0x0A);
    uint8_t v = Ports::inb(0x3D5);
    Ports::outb(0x3D5, visible ? (v & ~0x20) : (v | 0x20));
}
