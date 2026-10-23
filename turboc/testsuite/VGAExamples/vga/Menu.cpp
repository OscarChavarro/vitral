#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "programs/vga/Menu.h"
#include "programs/vga/PaletteSet.h"
#include "programs/vga/VgaKeyboard.h"

Menu::Menu() : selected(0), first(0) {
    status[0] = 0;
}

void
Menu::drawList() {
    if (selected < first) {
        first = selected;
    }
    if (selected >= first + MENU_ROWS) {
        first = selected - MENU_ROWS + 1;
    }
    char row[MENU_WIDTH + 1];
    for (int r = 0; r < MENU_ROWS; r++) {
        int at = first + r;
        row[0] = 0;
        if (at < modes.count()) {
            VgaModes::row(modes.at(at), row, sizeof(row));
        }
        printf("\033[%d;1H%s%-*s\033[0m", MENU_TOP + r, at == selected ? "\033[7m" : "", MENU_WIDTH, row);
    }
    int count = modes.count();
    int last = first + MENU_ROWS < count ? first + MENU_ROWS : count;
    int used = snprintf(row, sizeof(row), "%s modes %d-%d of %d %s", first > 0 ? "^" : " ", first + 1, last,
                        count, last < count ? "v" : " ");
    if (used < 0) {
        used = 0;
    }
    if (used > MENU_WIDTH) {
        used = MENU_WIDTH;
    }
    const char *suffix = "   ";
    for (; *suffix && used < MENU_WIDTH; suffix++) {
        row[used++] = *suffix;
    }
    for (const char *text = status; *text && used < MENU_WIDTH; text++) {
        row[used++] = *text;
    }
    row[used] = 0;
    printf("\033[%d;1H%-*s", MENU_TOP + MENU_ROWS, MENU_WIDTH, row);
    fflush(stdout);
}

void
Menu::draw(const PaletteSet &palettes) {
    fputs("\033[0m\033[?25l\033[2J\033[H", stdout);
    char header[MENU_WIDTH + 1];
    VgaModes::header(header, sizeof(header));
    printf("vga: the video modes of this machine, numbered and named as in svgalib");
    printf("\033[%d;1H%s", MENU_TOP - 1, header);
    printf("\033[%d;1HUp/Down: select  PgUp/PgDn: jump  Enter: show  ':' type a mode  ESC: quit",
           MENU_TOP + MENU_ROWS + 2);
    printf("\033[%d;1H", MENU_TOP + MENU_ROWS + 3);
    palettes.printLine();
    drawList();
}

int
Menu::typeMode(int *mode) {
    printf("\033[%d;1H%-*s\033[%d;1H\033[?25h", MENU_TOP + MENU_ROWS + 1, MENU_WIDTH, "", MENU_TOP + MENU_ROWS + 1);
    fflush(stdout);
    *mode = modes.prompt(status, sizeof(status));
    if (*mode < 0) {
        return 0;
    }
    selected = modes.indexOf(*mode);
    return 1;
}

int
Menu::choose(const PaletteSet &palettes, int *mode) {
    modes.rawKeys();
    draw(palettes);
    for (;;) {
        int key = VgaKeyboard::waitKey();
        if (key == KEY_ESC || key == 'q' || key == 'Q') {
            return 0;
        }
        if (key == '\n' || key == '\r') {
            if (modes.count() == 0) {
                continue;
            }
            *mode = modes.at(selected);
            return 1;
        }
        if (key == ':') {
            if (typeMode(mode)) {
                return 1;
            }
            draw(palettes);
            continue;
        }
        int to = selected;
        if (key == KEY_UP) {
            to--;
        } else if (key == KEY_DOWN) {
            to++;
        } else if (key == KEY_PAGE_UP) {
            to -= MENU_ROWS;
        } else if (key == KEY_PAGE_DOWN) {
            to += MENU_ROWS;
        } else {
            continue;
        }
        if (to < 0) {
            to = 0;
        }
        if (to > modes.count() - 1) {
            to = modes.count() - 1;
        }
        selected = to;
        status[0] = 0;
        drawList();
    }
}
