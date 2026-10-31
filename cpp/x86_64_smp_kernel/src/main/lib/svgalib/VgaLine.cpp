#include <vga.h>

// The svgalib API draws both endpoints with the color selected by vga_setcolor.
int
vga_drawline(int x1, int y1, int x2, int y2) {
    int dx = x2 >= x1 ? x2 - x1 : x1 - x2;
    int dy = y2 >= y1 ? y2 - y1 : y1 - y2;
    int sx = x1 < x2 ? 1 : -1;
    int sy = y1 < y2 ? 1 : -1;
    int error = dx - dy;
    for (;;) {
        vga_drawpixel(x1, y1);
        if (x1 == x2 && y1 == y2) {
            break;
        }
        int twice = error * 2;
        if (twice > -dy) {
            error -= dy;
            x1 += sx;
        }
        if (twice < dx) {
            error += dx;
            y1 += sy;
        }
    }
    return 0;
}
