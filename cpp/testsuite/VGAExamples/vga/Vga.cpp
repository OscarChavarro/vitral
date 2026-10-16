#include <new>
#include <stdio.h>

#include <vga.h>

#include "programs/vga/Vga.h"
#include "programs/vga/Picture.h"

int
Vga::showPicture(int mode) {
    VgaModes &modes = menu.videoModes();
    if (!modes.enter(mode)) {
        return 0;
    }
    Picture picture(modes.screen());
    if (picture.isGraphics() && picture.getColors() == 256) {
        palettes.apply();
    }
    picture.draw();
    if (picture.isGraphics()) {
        hud.loadFont();
    }
    mouse.place(picture, hud);
    hud.clear();
    hud.draw(picture, palettes, mouse);
    if (mouse.isConnected()) {
        mouse.cursor().show();
    }

    int escape = 0;
    for (;;) {
        int key = mouse.waitInput();
        if (key == KEY_MOUSE) {
            mouse.follow();
            hud.draw(picture, palettes, mouse);
            continue;
        }
        if (key == 'h' || key == 'H') {
            hud.toggle();
            hud.draw(picture, palettes, mouse);
            continue;
        }
        if (key == KEY_ESC) {
            escape = 1;
            break;
        }
        if (picture.getColors() == 256 && picture.isGraphics() && (key == KEY_LEFT || key == KEY_RIGHT)) {
            palettes.step(key == KEY_RIGHT ? 1 : -1);
            palettes.apply();
            hud.draw(picture, palettes, mouse);
            continue;
        }
        break;
    }
    mouse.cursor().hide();
    modes.leave();
    return escape;
}

int
Vga::run(int argc, char *argv[]) {
    int mode;
    palettes.scan();
    // svgalib saves the terminal mode at its first vga_setmode() and puts it back in TEXT, on a fatal signal and at
    // exit: let it save the one the program was started with, before the menu turns line editing and echo off.
    vga_setmode(TEXT);
    mouse.open();
    int status = 0;
    if (argc > 1) {
        VgaModes::Selection selection = menu.videoModes().select("vga", argc - 1, argv + 1, TEXT, &mode);
        if (selection == VgaModes::Selection::Chosen) {
            showPicture(mode);
        } else if (selection == VgaModes::Selection::Unknown) {
            status = 1;
        }
    } else {
        for (;;) {
            if (!menu.choose(palettes, &mode)) {
                break;
            }
            if (showPicture(mode)) {
                break;
            }
        }
        menu.videoModes().leave();
    }
    mouse.close();
    return status;
}

int
Vga::start(int argc, char *argv[]) {
    vga_setmousesupport(1);
    vga_init();
    Vga *vga = new (std::nothrow) Vga();
    if (!vga) {
        printf("vga: out of memory\n");
        return 1;
    }
    int status = vga->run(argc, argv);
    delete vga;
    return status;
}

int
main(int argc, char *argv[]) {
    return Vga::start(argc, argv);
}
