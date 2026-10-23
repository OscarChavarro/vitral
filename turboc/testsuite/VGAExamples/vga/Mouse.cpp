#include <stddef.h>
#include <stdio.h>
#include <unistd.h>

#include <vga.h>
#include <vgamouse.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "programs/vga/Mouse.h"
#include "programs/vga/Hud.h"
#include "programs/vga/Picture.h"
#include "programs/vga/VgaConstant.h"
#include "programs/vga/VgaKeyboard.h"

Mouse::Mouse() : connected(FALSE), picture(NULL), hud(NULL) {}

// vga_init() opens the mouse configured for svgalib (Vga::start asks for mouse support); it has capabilities only
// when that worked.
void
Mouse::open() {
    MouseCaps caps;
    connected = mouse_getcaps(&caps) == 0;
}

void
Mouse::close() {
    if (connected) {
        mouse_close();
        connected = FALSE;
    }
}

int
Mouse::waitInput() {
    for (;;) {
        int key = VgaKeyboard::readKey();
        if (key) {
            return key;
        }
        int events = vga_waitevent(VGA_KEYEVENT | (connected ? VGA_MOUSEEVENT : 0), NULL, NULL, NULL, NULL);
        if (events < 0) {
            usleep(20000);
        } else if (events & VGA_MOUSEEVENT) {
            return KEY_MOUSE;
        }
    }
}

void
Mouse::follow() { pointer.moveTo(mouse_getx(), mouse_gety()); }

// The mouse range becomes the picture (cells in text mode, at TEXT_SCALE counts each), the pointer starts in the
// middle and the wheel, svgalib's rx axis, adds up its turns from zero.
void
Mouse::place(const Picture &shown, const Hud &overlay) {
    if (!connected) {
        return;
    }
    picture = &shown;
    hud = &overlay;
    mouse_update();
    int width = shown.getWidth();
    int height = shown.getHeight();
    if (shown.isGraphics()) {
        mouse_setscale(1);
        pointer.useGraphics(width, height, shown.getWhite(), 0);
    } else {
        mouse_setscale(TEXT_SCALE);
        pointer.useText(width, height, *this);
    }
    mouse_setxrange(0, width - 1);
    mouse_setyrange(0, height - 1);
    mouse_setwrap(MOUSE_ROT_RX_RY_RZ);
    mouse_setrange_6d(0, 0, 0, 0, 0, 0, -WHEEL_RANGE, WHEEL_RANGE, 0, 0, 0, 0, MOUSE_RXDIM);
    mouse_setposition_6d(width / 2, height / 2, 0, 0, 0, 0, MOUSE_XDIM | MOUSE_YDIM | MOUSE_RXDIM);
}

void
Mouse::describe(char *position, char *state, size_t size) const {
    if (!connected) {
        return;
    }
    int buttons = mouse_getbutton();
    int wheel = 0;
    mouse_getposition_6d(NULL, NULL, NULL, &wheel, NULL, NULL);
    snprintf(position, size, "Mouse (x, y): (%d, %d)", pointer.x(), pointer.y());
    snprintf(state, size, "Buttons: %c %c %c  Wheel: %d", buttons & MOUSE_LEFTBUTTON ? 'L' : '-',
             buttons & MOUSE_MIDDLEBUTTON ? 'M' : '-', buttons & MOUSE_RIGHTBUTTON ? 'R' : '-', wheel);
}

// The text picture: the HUD lines (inverse video) over the circle (bold dots).
char
Mouse::cellAt(int column, int row, BOOLEAN &bold, BOOLEAN &inverse) const {
    char c;
    if (hud && hud->cellAt(column, row, c)) {
        bold = FALSE;
        inverse = TRUE;
        return c;
    }
    bold = picture && picture->covers(column, row);
    inverse = FALSE;
    return bold ? '.' : ' ';
}
