#include "programs/vitralRaytracer/RaytracerKeyboard.h"
#include "programs/vitralRaytracer/RaytracerConstant.h"
#include <unistd.h>
#include <vga.h>

int
RaytracerKeyboard::waitKey() {
    for (;;) {
        int key = vga_getkey();
        if (key == 0) {
            usleep(20000);
            continue;
        }
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
        return KEY_OTHER;
    }
}
