#include "RasterizerKeyboard.h"
#include "RasterizerKey.h"
#include <unistd.h>
#include <vga.h>

int
RasterizerKeyboard::waitKey() {
    for (;;) {
        int key = vga_getkey();
        if (key == 0) {
            usleep(20000);
            continue;
        }
        if (key != static_cast<int>(RasterizerKey::Escape)) {
            return key;
        }
        int next = vga_getkey();
        if (next == 0) {
            return static_cast<int>(RasterizerKey::Escape);
        }
        if (next != '[') {
            return static_cast<int>(RasterizerKey::Other);
        }
        int code = vga_getkey();
        if (code == 'A') {
            return static_cast<int>(RasterizerKey::Up);
        }
        if (code == 'B') {
            return static_cast<int>(RasterizerKey::Down);
        }
        if (code == 'C') {
            return static_cast<int>(RasterizerKey::Right);
        }
        if (code == 'D') {
            return static_cast<int>(RasterizerKey::Left);
        }
        return static_cast<int>(RasterizerKey::Other);
    }
}

