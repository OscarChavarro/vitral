#include <stddef.h>
#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiPointer.h"

static const char *const ARROW_ROWS[BgiPointer::HEIGHT] = {
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

BgiPointer::BgiPointer()
    : surface(NULL), fillColor(0), outlineColor(0), posX(0), posY(0), visible(FALSE), savedX(0), savedY(0)
{
}

void
BgiPointer::use(Framebuffer *target, unsigned long fill, unsigned long outline)
{
    hide();
    surface = target;
    fillColor = fill;
    outlineColor = outline;
}

void
BgiPointer::moveTo(int x, int y)
{
    if ( x == posX && y == posY ) {
        return;
    }
    BOOLEAN wasVisible = visible;
    hide();
    posX = x;
    posY = y;
    if ( wasVisible ) {
        show();
    }
}

void
BgiPointer::show()
{
    if ( visible || !surface ) {
        return;
    }
    draw();
    visible = TRUE;
}

void
BgiPointer::hide()
{
    if ( !visible ) {
        return;
    }
    restore();
    visible = FALSE;
}

void
BgiPointer::draw()
{
    savedX = posX;
    savedY = posY;
    int row;
    for ( row = 0; row < HEIGHT; row++ ) {
        int column;
        for ( column = 0; column < WIDTH; column++ ) {
            int x = savedX + column;
            int y = savedY + row;
            char c = ARROW_ROWS[row][column];
            if ( c == ' ' || x >= surface->width() || y >= surface->height() ) {
                continue;
            }
            saved[row * WIDTH + column] = surface->nativePixel(x, y);
            surface->putNativePixel(x, y, c == '#' ? outlineColor : fillColor);
        }
    }
}

void
BgiPointer::restore()
{
    int row;
    for ( row = 0; row < HEIGHT; row++ ) {
        int column;
        for ( column = 0; column < WIDTH; column++ ) {
            int x = savedX + column;
            int y = savedY + row;
            if ( ARROW_ROWS[row][column] == ' ' || x >= surface->width() || y >= surface->height() ) {
                continue;
            }
            surface->putNativePixel(x, y, saved[row * WIDTH + column]);
        }
    }
}
