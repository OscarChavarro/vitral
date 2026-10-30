#include "lib/mouseCursor/MouseCursor.h"
#include <stdio.h>
#include <vga.h>

static const char *const ARROW_ROWS[] = {
    "#           ",
    "##          ",
    "#.#         ",
    "#..#        ",
    "#...#       ",
    "#....#      ",
    "#.....#     ",
    "#......#    ",
    "#.......#   ",
    "#........#  ",
    "#.........# ",
    "#......#####",
    "#...#..#    ",
    "#..##..#    ",
    "#.#  #..#   ",
    "##   #..#   ",
    "#     #..#  ",
    "      #..#  ",
    "       ##   ",
};

static const char *const CROSSHAIR_ROWS[] = {
    "     ###     ",
    "     #.#     ",
    "     #.#     ",
    "     #.#     ",
    "     #.#     ",
    "######.######",
    "#.....#.....#",
    "######.######",
    "     #.#     ",
    "     #.#     ",
    "     #.#     ",
    "     #.#     ",
    "     ###     ",
};

const MouseCursor::Shape MouseCursor::ARROW = {12, 19, 0, 0, ARROW_ROWS};
const MouseCursor::Shape MouseCursor::CROSSHAIR = {13, 13, 6, 6, CROSSHAIR_ROWS};

MouseCursor::MouseCursor()
    : graphicsMode(false), width(80), height(25), fillColor(15), outlineColor(0), shape(&ARROW), text(nullptr),
      posX(40), posY(12), visible(false), savedX(0), savedY(0), current(0) {}

void
MouseCursor::useGraphics(int screenWidth, int screenHeight, int fill, int outline, const Shape &image) {
    hide();
    graphicsMode = true;
    width = screenWidth;
    height = screenHeight;
    fillColor = fill;
    outlineColor = outline;
    shape = &image;
    posX = width / 2;
    posY = height / 2;
}

void
MouseCursor::useText(int columns, int rows, const TextScreen &screen) {
    hide();
    graphicsMode = false;
    width = columns;
    height = rows;
    text = &screen;
    posX = width / 2;
    posY = height / 2;
}

void
MouseCursor::moveTo(int x, int y) {
    if (x < 0) {
        x = 0;
    }
    if (x >= width) {
        x = width - 1;
    }
    if (y < 0) {
        y = 0;
    }
    if (y >= height) {
        y = height - 1;
    }
    if (x == posX && y == posY) {
        return;
    }
    if (visible && graphicsMode) {
        move(x, y);
        return;
    }
    if (visible) {
        restore();
    }
    posX = x;
    posY = y;
    if (visible) {
        draw();
    }
}

void
MouseCursor::show() {
    if (visible) {
        return;
    }
    draw();
    visible = true;
}

void
MouseCursor::hide() {
    if (!visible) {
        return;
    }
    restore();
    visible = false;
}

bool
MouseCursor::overlaps(int x, int y, int w, int h) const {
    if (!visible) {
        return false;
    }
    if (!graphicsMode) {
        return posX >= x && posX < x + w && posY >= y && posY < y + h;
    }
    int shapeWidth = shape->width < MAX_SIZE ? shape->width : MAX_SIZE;
    int shapeHeight = shape->height < MAX_SIZE ? shape->height : MAX_SIZE;
    return savedX < x + w && x < savedX + shapeWidth && savedY < y + h && y < savedY + shapeHeight;
}

char
MouseCursor::pixelAt(int originX, int originY, int x, int y) const {
    if (x < 0 || y < 0 || x >= width || y >= height) {
        return ' ';
    }
    int column = x - originX;
    int row = y - originY;
    if (column < 0 || row < 0 || column >= shape->width || row >= shape->height || column >= MAX_SIZE ||
        row >= MAX_SIZE) {
        return ' ';
    }
    return shape->rows[row][column];
}

void
MouseCursor::draw() {
    if (!graphicsMode) {
        writeCell(true);
        return;
    }
    savedX = posX - shape->hotX;
    savedY = posY - shape->hotY;
    int *under = saved[current];
    for (int row = 0; row < MAX_SIZE; row++) {
        for (int column = 0; column < MAX_SIZE; column++) {
            if (pixelAt(savedX, savedY, savedX + column, savedY + row) != ' ') {
                under[row * MAX_SIZE + column] = vga_getpixel(savedX + column, savedY + row);
            }
        }
    }
    int color = -1;
    for (int row = 0; row < MAX_SIZE; row++) {
        for (int column = 0; column < MAX_SIZE; column++) {
            char c = pixelAt(savedX, savedY, savedX + column, savedY + row);
            if (c == ' ') {
                continue;
            }
            if (colorOf(c) != color) {
                color = colorOf(c);
                vga_setcolor(color);
            }
            vga_drawpixel(savedX + column, savedY + row);
        }
    }
}

void
MouseCursor::restore() {
    if (!graphicsMode) {
        writeCell(false);
        return;
    }
    const int *under = saved[current];
    for (int row = 0; row < MAX_SIZE; row++) {
        for (int column = 0; column < MAX_SIZE; column++) {
            if (pixelAt(savedX, savedY, savedX + column, savedY + row) == ' ') {
                continue;
            }
            vga_setcolor(under[row * MAX_SIZE + column]);
            vga_drawpixel(savedX + column, savedY + row);
        }
    }
}

// Moves a visible graphics cursor touching only the frame buffer pixels that change: where the old and the new
// cursor overlap, the saved background is copied in memory instead of read back, the old cursor is erased only
// where the new one does not cover it, and the new one is drawn only where the old one left another color.
void
MouseCursor::move(int x, int y) {
    int oldX = savedX;
    int oldY = savedY;
    int newX = x - shape->hotX;
    int newY = y - shape->hotY;
    const int *before = saved[current];
    int *after = saved[1 - current];
    for (int row = 0; row < MAX_SIZE; row++) {
        for (int column = 0; column < MAX_SIZE; column++) {
            int screenX = newX + column;
            int screenY = newY + row;
            if (pixelAt(newX, newY, screenX, screenY) == ' ') {
                continue;
            }
            if (pixelAt(oldX, oldY, screenX, screenY) != ' ') {
                after[row * MAX_SIZE + column] = before[(screenY - oldY) * MAX_SIZE + screenX - oldX];
            } else {
                after[row * MAX_SIZE + column] = vga_getpixel(screenX, screenY);
            }
        }
    }
    int color = -1;
    for (int row = 0; row < MAX_SIZE; row++) {
        for (int column = 0; column < MAX_SIZE; column++) {
            int screenX = oldX + column;
            int screenY = oldY + row;
            if (pixelAt(oldX, oldY, screenX, screenY) == ' ' || pixelAt(newX, newY, screenX, screenY) != ' ') {
                continue;
            }
            if (before[row * MAX_SIZE + column] != color) {
                color = before[row * MAX_SIZE + column];
                vga_setcolor(color);
            }
            vga_drawpixel(screenX, screenY);
        }
    }
    for (int row = 0; row < MAX_SIZE; row++) {
        for (int column = 0; column < MAX_SIZE; column++) {
            int screenX = newX + column;
            int screenY = newY + row;
            char c = pixelAt(newX, newY, screenX, screenY);
            if (c == ' ') {
                continue;
            }
            char old = pixelAt(oldX, oldY, screenX, screenY);
            if (old != ' ' && colorOf(old) == colorOf(c)) {
                continue;
            }
            if (colorOf(c) != color) {
                color = colorOf(c);
                vga_setcolor(color);
            }
            vga_drawpixel(screenX, screenY);
        }
    }
    savedX = newX;
    savedY = newY;
    current = 1 - current;
    posX = x;
    posY = y;
}

// Writes the text cell under the pointer as the screen has it, or in the opposite video when it is the pointer.
void
MouseCursor::writeCell(bool pointer) const {
    bool bold = false;
    bool inverse = false;
    char c = text ? text->cellAt(posX, posY, bold, inverse) : ' ';
    if (pointer) {
        inverse = !inverse;
    }
    printf("\033[%d;%dH\033[0m%s%s%c\033[0m", posY + 1, posX + 1, bold ? "\033[1m" : "", inverse ? "\033[7m" : "", c);
    fflush(stdout);
}
