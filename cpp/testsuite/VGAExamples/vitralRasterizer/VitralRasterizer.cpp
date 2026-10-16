#include <new>
#include <vga.h>

#include "VitralRasterizer.h"
#include "RasterizerDisplay.h"
#include "RasterizerExamples.h"
#include "RasterizerKeyboard.h"
#include "RasterizerKey.h"
#include "RasterizerPalette.h"

VitralRasterizer::VitralRasterizer(VgaModes *modes)
    : modes(modes), colorMode(ColorMode::FLOYD_STEINBERG_OPTIMIZED_PALETTE), example(0) {}

int
VitralRasterizer::waitCommand() {
    for (;;) {
        int key = RasterizerKeyboard::waitKey();
        switch (key) {
        case static_cast<int>(RasterizerKey::Escape):
        case static_cast<int>(RasterizerKey::Left):
        case static_cast<int>(RasterizerKey::Right):
        case static_cast<int>(RasterizerKey::Up):
        case static_cast<int>(RasterizerKey::Down):
        case 'r':
        case 'R':
        case 'b':
        case 'B':
        case ' ':
        case 'h':
        case 'H': return key;
        default: break;
        }
    }
}

int
VitralRasterizer::run(int mode) {
    if (!modes->enter(mode)) {
        return 1;
    }
    hud.loadFont();
    RasterizerPalette::makePalette(modes->screen().colors, &palette);
    for (;;) {
        RasterizerShown shown;
        if (RasterizerDisplay::showExample(example, modes->screen().width, modes->screen().height, &palette, colorMode,
                                           &shown)) {
            hud.draw(&shown, colorMode, modes->screen().width, modes->screen().height, modes->screen().text);
        }
        int key = waitCommand();
        if (key == static_cast<int>(RasterizerKey::Escape)) {
            break;
        }
        if (key == 'h' || key == 'H') {
            hud.toggle();
        } else if (key == ' ') {
            colorMode = RasterizerPalette::nextColorMode(colorMode, &palette);
        } else if (key == static_cast<int>(RasterizerKey::Left) || key == static_cast<int>(RasterizerKey::Right)) {
            int step = key == static_cast<int>(RasterizerKey::Right) ? 1 : RasterizerExamples::COUNT - 1;
            example = (example + step) % RasterizerExamples::COUNT;
        } else {
            bool down = key == 'R' || key == static_cast<int>(RasterizerKey::Down);
            VgaModes::Change change = key == 'b' || key == 'B' ? modes->nextDepth() : modes->step(down ? 1 : -1);
            if (change == VgaModes::Change::None) {
                continue;
            }
            if (change == VgaModes::Change::Failed) {
                break;
            }
            RasterizerPalette::makePalette(modes->screen().colors, &palette);
            if (!hud.hasFont()) {
                hud.loadFont();
            }
        }
    }
    modes->leave();
    return 0;
}

int
VitralRasterizer::start(int argc, char *argv[]) {
    vga_init();
    VgaModes modes(false);
    int mode;
    int fallback = modes.count() > 0 ? modes.highest() : G320x240x256;
    switch (modes.select("vitral_rasterizer", argc - 1, argv + 1, fallback, &mode)) {
    case VgaModes::Selection::Quit: return 0;
    case VgaModes::Selection::Unknown: return 1;
    case VgaModes::Selection::Chosen: break;
    }
    VitralRasterizer *rasterizer = new (std::nothrow) VitralRasterizer(&modes);
    if (!rasterizer) {
        return 1;
    }
    int status = rasterizer->run(mode);
    delete rasterizer;
    return status;
}

int
main(int argc, char *argv[]) {
    return VitralRasterizer::start(argc, argv);
}
