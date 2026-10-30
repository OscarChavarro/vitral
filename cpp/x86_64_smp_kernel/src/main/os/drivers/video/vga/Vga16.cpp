#include "drivers/video/vga/Vga16.h"
#include "lib/vitralKernel/Ports.h"

int Vga16::cx = 0;
int Vga16::cy = 0;
uint8_t Vga16::attr = 0x07;

void
Vga16::cell_write(int idx, uint16_t v) {
    asm volatile("pushw %%fs\n\t"
                 "movw %0, %%fs\n\t"
                 "movw %1, %%fs:(%%bx)\n\t"
                 "popw %%fs"
                 : : "r"(SEG), "r"(v), "b"((uint16_t)(idx * 2)) : "memory");
}

uint16_t
Vga16::cell_read(int idx) {
    uint16_t v;
    asm volatile("pushw %%fs\n\t"
                 "movw %1, %%fs\n\t"
                 "movw %%fs:(%%bx), %0\n\t"
                 "popw %%fs"
                 : "=r"(v) : "r"(SEG), "b"((uint16_t)(idx * 2)) : "memory");
    return v;
}

void
Vga16::update_cursor() {
    uint16_t pos = (uint16_t)(cy * COLS + cx);
    Ports::outb(0x3D4, 0x0F); Ports::outb(0x3D5, (uint8_t)pos);
    Ports::outb(0x3D4, 0x0E); Ports::outb(0x3D5, (uint8_t)(pos >> 8));
}

void
Vga16::scroll() {
    for (int i = 0; i < (ROWS - 1) * COLS; i++) {
        cell_write(i, cell_read(i + COLS));
    }
    for (int i = (ROWS - 1) * COLS; i < ROWS * COLS; i++) {
        cell_write(i, (uint16_t)(attr << 8) | ' ');
    }
    cy = ROWS - 1;
}

void
Vga16::set_attr(uint8_t a) { attr = a; }

void
Vga16::clear(uint8_t a) {
    attr = a;
    for (int i = 0; i < ROWS * COLS; i++) {
        cell_write(i, (uint16_t)(attr << 8) | ' ');
    }
    cx = cy = 0;
    update_cursor();
}

void
Vga16::putc(char c) {
    if (c == '\n') {
        cx = 0;
        cy++;
    } else if (c == '\r') {
        cx = 0;
    } else if (c == '\t') {
        cx = (cx + 8) & ~7;
    } else {
        cell_write(cy * COLS + cx, (uint16_t)(attr << 8) | (uint8_t)c);
        cx++;
    }
    if (cx >= COLS) {
        cx = 0;
        cy++;
    }
    if (cy >= ROWS) {
        scroll();
    }
    update_cursor();
}
