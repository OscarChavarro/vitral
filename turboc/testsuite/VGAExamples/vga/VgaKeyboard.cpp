#include <unistd.h>

#include <vga.h>

#include "programs/vga/VgaKeyboard.h"
#include "programs/vga/VgaConstant.h"

int
VgaKeyboard::readKey() {
    int key = vga_getkey();
    if (key != KEY_ESC) {
        return key;
    }
    int next = vga_getkey();
    if (next == 0) {
        return KEY_ESC;
    }
    if (next != '[') {
        return KEY_OTHER;
    }
    int code = vga_getkey();
    if (code == 'A') {
        return KEY_UP;
    }
    if (code == 'B') {
        return KEY_DOWN;
    }
    if (code == 'C') {
        return KEY_RIGHT;
    }
    if (code == 'D') {
        return KEY_LEFT;
    }
    if ((code == '5' || code == '6') && vga_getkey() == '~') {
        return code == '5' ? KEY_PAGE_UP : KEY_PAGE_DOWN;
    }
    return KEY_OTHER;
}

int
VgaKeyboard::waitKey() {
    for (;;) {
        int key = readKey();
        if (key) {
            return key;
        }
        usleep(20000);
    }
}
