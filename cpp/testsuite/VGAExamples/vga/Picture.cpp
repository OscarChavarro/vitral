#include <stdio.h>

#include <vga.h>

#include "programs/vga/Picture.h"
#include "programs/vga/HudPatch.h"
#include "programs/vga/VgaConstant.h"

Picture::Picture(const VgaModes::Screen &screen)
    : graphics(!screen.text), width(screen.width), height(screen.height), colors(screen.colors),
      bitsPerPixel(screen.bitsPerPixel), white(screen.colors - 1), square(screen.width >= 800) {
    cx = width / 2;
    cy = height / 2;
    radius = 3 * width / 8 - 2;
    if (graphics) {
        int space = height - legendHeight() - 4;
        int vertical = space / 2 - 2;
        int fit = square ? vertical : vertical * 3 * width / (4 * height);
        if (fit < radius) {
            radius = fit;
        }
        cy = space / 2;
    }
}

void
Picture::plot(int x, int y, const HudPatch *patch) const {
    if (x < 0 || y < 0 || x >= width || y >= height) {
        return;
    }
    if (patch && !patch->uncovers(x, y)) {
        return;
    }
    if (graphics) {
        vga_drawpixel(x, y);
    } else {
        printf("\033[%d;%dH.", y + 1, x + 1);
    }
}

int
Picture::rows(int v) const {
    if (square) {
        return v;
    }
    return (v * 4 * height + 3 * width / 2) / (3 * width);
}

void
Picture::plot8(int x, int y, const HudPatch *patch) const {
    plot(cx + x, cy + rows(y), patch);
    plot(cx - x, cy + rows(y), patch);
    plot(cx + x, cy - rows(y), patch);
    plot(cx - x, cy - rows(y), patch);
    plot(cx + y, cy + rows(x), patch);
    plot(cx - y, cy + rows(x), patch);
    plot(cx + y, cy - rows(x), patch);
    plot(cx - y, cy - rows(x), patch);
}

void
Picture::circle(const HudPatch *patch) const {
    int x = 0;
    int y = radius;
    int d = 3 - 2 * radius;
    while (x <= y) {
        plot8(x, y, patch);
        if (d < 0) {
            d += 4 * x + 6;
        } else {
            d += 4 * (x - y) + 10;
            y--;
        }
        x++;
    }
}

// Whether the circle has a point at (px, py): the walk of circle(), comparing instead of plotting.
bool
Picture::covers(int px, int py) const {
    int x = 0;
    int y = radius;
    int d = 3 - 2 * radius;
    while (x <= y) {
        if (hits8(x, y, px, py)) {
            return true;
        }
        if (d < 0) {
            d += 4 * x + 6;
        } else {
            d += 4 * (x - y) + 10;
            y--;
        }
        x++;
    }
    return false;
}

bool
Picture::hits8(int x, int y, int px, int py) const {
    bool steep = (px == cx + x || px == cx - x) && (py == cy + rows(y) || py == cy - rows(y));
    bool flat = (px == cx + y || px == cx - y) && (py == cy + rows(x) || py == cy - rows(x));
    return steep || flat;
}

void
Picture::fillRect(int x, int y, int w, int h, int color) {
    vga_setcolor(color);
    for (int j = 0; j < h; j++) {
        for (int i = 0; i < w; i++) {
            vga_drawpixel(x + i, y + j);
        }
    }
}

int
Picture::legendHeight() const {
    if (!graphics) {
        return 0;
    }
    if (colors > 256) {
        return 0;
    }
    if (colors == 256) {
        return SWATCH;
    }
    int rowCount = colors > 8 ? 2 : 1;
    return rowCount * (SWATCH + 1) + 1 + 2;
}

void
Picture::drawLegend() const {
    int h = height;
    if (colors > 256) {
        return;
    }
    if (colors == 256) {
        for (int c = 0; c < 256; c++) {
            fillRect(c, h - SWATCH, 1, SWATCH, c);
        }
        return;
    }
    int cols = colors > 8 ? 8 : colors;
    int rowCount = colors / cols;
    int pitch = SWATCH + 1;
    int x0 = 2;
    int y0 = h - 2 - (rowCount * pitch + 1);
    fillRect(x0, y0, cols * pitch + 1, rowCount * pitch + 1, white);
    for (int c = 0; c < colors; c++) {
        fillRect(x0 + 1 + c % cols * pitch, y0 + 1 + c / cols * pitch, SWATCH, SWATCH, c);
    }
}

void
Picture::draw() const {
    if (graphics) {
        vga_setcolor(white);
    } else {
        fputs("\033[1m", stdout);
    }
    circle(nullptr);
    if (graphics) {
        drawLegend();
    }
    fflush(stdout);
}
