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
        if (key != ((int)(RasterizerKey::Escape))) {
            return key;
        }
        int next = vga_getkey();
        if (next == 0) {
            return ((int)(RasterizerKey::Escape));
        }
        if (next != '[') {
            return ((int)(RasterizerKey::Other));
        }
        int code = vga_getkey();
        if (code == 'A') {
            return ((int)(RasterizerKey::Up));
        }
        if (code == 'B') {
            return ((int)(RasterizerKey::Down));
        }
        if (code == 'C') {
            return ((int)(RasterizerKey::Right));
        }
        if (code == 'D') {
            return ((int)(RasterizerKey::Left));
        }
        return ((int)(RasterizerKey::Other));
    }
}

