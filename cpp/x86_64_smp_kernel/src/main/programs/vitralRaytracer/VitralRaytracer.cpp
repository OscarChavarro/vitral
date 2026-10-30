#include <new>
#include <stdio.h>
#include <unistd.h>
#include <vga.h>

#include "programs/vitralRaytracer/VitralRaytracer.h"
#include "programs/vitralRaytracer/RaytracerDisplay.h"
#include "programs/vitralRaytracer/RaytracerKeyboard.h"
#include "programs/vitralRaytracer/RaytracerPalette.h"
#include "programs/vitralRaytracer/Shown.h"
#include "vsdk/toolkit/common/VSDK.h"

#ifdef VITRAL_RAYTRACER_KERNEL_LOG
int
kprintf(const char *fmt, ...);
#endif

VitralRaytracer::VitralRaytracer(VgaModes *modes)
    : modes(modes), width(0), height(0), colorMode(FLOYD_STEINBERG_OPTIMIZED_PALETTE), seconds(0), parallel(0) {
    error[0] = 0;
}

void
VitralRaytracer::useScreen() {
    const VgaModes::Screen &screen = modes->screen();
    width = screen.text ? TEXT_RENDER_WIDTH : screen.width;
    height = screen.text ? TEXT_RENDER_HEIGHT : screen.height;
    RaytracerPalette::makePalette(screen.colors, &palette);
}

int
VitralRaytracer::waitCommand() {
    for (;;) {
        int key = RaytracerKeyboard::waitKey();
        switch (key) {
        case KEY_ESC:
        case KEY_LEFT:
        case KEY_RIGHT:
        case KEY_UP:
        case KEY_DOWN:
        case 'r':
        case 'R':
        case 'b':
        case 'B':
        case ' ':
        case 'p':
        case 'P':
        case 'h':
        case 'H': return key;
        default: break;
        }
    }
}

int
VitralRaytracer::run(int mode, const char *scene) {
    scenes.list(scene);
#ifdef VITRAL_RAYTRACER_KERNEL_LOG
    kprintf("vitral_raytracer: selecting svgalib mode %d (%s)\n", mode, VgaModes::name(mode));
#endif
    if (!modes->enter(mode)) {
#ifdef VITRAL_RAYTRACER_KERNEL_LOG
        kprintf("vitral_raytracer: svgalib mode %d failed\n", mode);
#endif
        return 1;
    }
    useScreen();
#ifdef VITRAL_RAYTRACER_KERNEL_LOG
    const VgaModes::Screen &selectedScreen = modes->screen();
    kprintf("vitral_raytracer: entered mode %d (%s), %dx%d %dbpp%s\n", selectedScreen.mode,
            VgaModes::name(selectedScreen.mode), selectedScreen.width, selectedScreen.height,
            selectedScreen.bitsPerPixel, selectedScreen.text ? " text" : " graphics");
#endif
    hud.loadFont();
    int rerender = 1;
    parallel = sysconf(_SC_NPROCESSORS_ONLN) > 1;
    for (;;) {
        if (rerender) {
            error[0] = 0;
            frame.render(&scenes, width, height, modes->screen().text, parallel, &palette, &seconds, error,
                         sizeof(error));
            rerender = 0;
        }
        if (frame.isValid()) {
            Shown shown;
            double vgaSeconds;
            if (RaytracerDisplay::showImage(frame.pixels(), width, height, modes->screen().text, &palette, colorMode,
                                            &shown, &vgaSeconds)) {
                hud.draw(&shown, colorMode, width, height, modes->screen().text, seconds);
#ifdef VITRAL_RAYTRACER_KERNEL_LOG
                const VgaModes::Screen &screen = modes->screen();
                unsigned computeHundredths = (unsigned)(seconds * 100.0 + 0.5);
                unsigned vgaHundredths = (unsigned)(vgaSeconds * 100.0 + 0.5);
                unsigned totalHundredths = (unsigned)((seconds + vgaSeconds) * 100.0 + 0.5);
                kprintf("vitral_raytracer: %dx%d %dbpp compute: %u.%02us vga: %u.%02us total: %u.%02us\n",
                        screen.width, screen.height, screen.bitsPerPixel, computeHundredths / 100,
                        computeHundredths % 100, vgaHundredths / 100, vgaHundredths % 100, totalHundredths / 100,
                        totalHundredths % 100);
#endif
            }
        }
        int key = waitCommand();
        if (key == KEY_ESC) {
            break;
        }
        if (key == 'h' || key == 'H') {
            hud.toggle();
        } else if (key == 'p' || key == 'P') {
            parallel = !parallel;
            rerender = 1;
        } else if (key == ' ') {
            colorMode = RaytracerPalette::nextColorMode(colorMode, &palette);
        } else if (key == KEY_LEFT || key == KEY_RIGHT) {
            scenes.step(key == KEY_RIGHT ? 1 : -1);
            rerender = 1;
        } else {
            VgaModes::Change change = key == 'b' || key == 'B' ? modes->nextDepth()
                                                               : modes->step(key == 'R' || key == KEY_DOWN ? 1 : -1);
            if (change == VgaModes::Change::None) {
                continue;
            }
            if (change == VgaModes::Change::Failed) {
#ifdef VITRAL_RAYTRACER_KERNEL_LOG
                kprintf("vitral_raytracer: svgalib mode change failed\n");
#endif
                break;
            }
            useScreen();
#ifdef VITRAL_RAYTRACER_KERNEL_LOG
            const VgaModes::Screen &changedScreen = modes->screen();
            kprintf("vitral_raytracer: changed to mode %d (%s), %dx%d %dbpp%s\n", changedScreen.mode,
                    VgaModes::name(changedScreen.mode), changedScreen.width, changedScreen.height,
                    changedScreen.bitsPerPixel, changedScreen.text ? " text" : " graphics");
#endif
            if (!hud.hasFont()) {
                hud.loadFont();
            }
            rerender = 1;
        }
    }
    modes->leave();
    char path[SceneList::PATH_SIZE];
    scenes.path(path);
    if (error[0]) {
        printf("vitral_raytracer: %s\n", error);
    } else {
        printf("vitral_raytracer: %s rendered at %dx%d in %s seconds (%s).\n", path, frame.frameWidth(),
               frame.frameHeight(), VSDK::formatDouble(seconds, 3).c_str(), parallel ? "parallel" : "serial");
    }
    return 0;
}

int
VitralRaytracer::start(int argc, char *argv[]) {
    const char *scene = DEFAULT_SCENE;
    int first = 1;
    vga_init();
    VgaModes modes;
    if (argc > 1 && SceneList::endsWithRay(argv[1])) {
        scene = argv[1];
        first = 2;
    }
    int mode;
    int fallback = modes.count() > 0 ? modes.highest() : TEXT;
    switch (modes.select("vitral_raytracer", argc - first, argv + first, fallback, &mode)) {
    case VgaModes::Selection::Quit: return 0;
    case VgaModes::Selection::Unknown: return 1;
    case VgaModes::Selection::Chosen: break;
    }
    VitralRaytracer *raytracer = new (std::nothrow) VitralRaytracer(&modes);
    if (!raytracer) {
        return 1;
    }
    int status = raytracer->run(mode, scene);
    delete raytracer;
    return status;
}

int
main(int argc, char *argv[]) {
    return VitralRaytracer::start(argc, argv);
}
