#include <new>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <vga.h>

#include "application/framebuffer/FramebufferSceneEditor.h"
#include "java/lang/Exception.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "model/GuiState.h"
#include "programs/sceneEditor/SceneEditor.h"

static const int SUPPORTED_DEPTHS[] = {4, 8, 16, 24};
// Only used without direct colour modes: the raytraced views need more colours than a palette has.
static const int PALETTE_MODES[] = {G1024x768x256, G800x600x256, G640x480x256, G640x480x16};
static const char *const DEFAULT_LANGUAGE = "english";

SceneEditor::SceneEditor() : modes(false) {
    modes.keepDepths(SUPPORTED_DEPTHS, (int)(sizeof(SUPPORTED_DEPTHS) / sizeof(SUPPORTED_DEPTHS[0])));
}

// The available 16 or 24 bits per pixel mode with the most pixels; at the same size, the one with more colours.
int
SceneEditor::largestDirectColourMode() const {
    int best = -1;
    int bestBits = 0;
    unsigned bestArea = 0;
    for (int i = 0; i < modes.count(); i++) {
        int mode = modes.at(i);
        int bits = VgaModes::bitsPerPixel(mode);
        const vga_modeinfo *info = vga_getmodeinfo(mode);
        if ((bits != 16 && bits != 24) || !info) {
            continue;
        }
        unsigned area = (unsigned)info->width * (unsigned)info->height;
        if (area > bestArea || (area == bestArea && bits > bestBits)) {
            best = mode;
            bestBits = bits;
            bestArea = area;
        }
    }
    return best;
}

int
SceneEditor::initialMode() const {
    int directColour = largestDirectColourMode();
    if (directColour >= 0) {
        return directColour;
    }
    for (int mode : PALETTE_MODES) {
        if (modes.indexOf(mode) >= 0) {
            return mode;
        }
    }
    return modes.count() > 0 ? modes.highest() : -1;
}

void
SceneEditor::usage() const {
    printf("usage: sceneEditor [LANGUAGE] [MODE]\n");
    printf("LANGUAGE: a GUI definition of etc/gui (english, spanish...)\n");
    printf("MODE: one of the graphics modes with 4, 8, 16 or 24 bits per pixel:\n");
    modes.print("Modes");
    int fallback = initialMode();
    printf("\nDefault mode: %s\n", fallback >= 0 ? VgaModes::name(fallback) : "none");
}

bool
SceneEditor::isKnownLanguage(const char *language) {
    java::ArrayList<java::String> languages = GuiState::listLanguages();
    for (long i = 0; i < languages.size(); i++) {
        if (languages.get(i).equals(language)) {
            return true;
        }
    }
    return false;
}

int
SceneEditor::run(int argc, char *argv[]) {
    const char *language = DEFAULT_LANGUAGE;
    int mode = -1;
    for (int i = 1; i < argc; i++) {
        int asked = VgaModes::lookup(argv[i]);
        if (asked > 0 && modes.indexOf(asked) >= 0) {
            mode = asked;
        } else if (asked > 0 || argv[i][0] == '-') {
            usage();
            return 1;
        } else {
            language = argv[i];
        }
    }
    // The application reads ../../../../etc, as from its folder in the repository.
    if (chdir(SCENE_EDITOR_APPLICATION_DIR) != 0) {
        printf("sceneEditor: cannot enter %s\n", SCENE_EDITOR_APPLICATION_DIR);
        return 1;
    }
    if (!isKnownLanguage(language)) {
        printf("sceneEditor: unknown GUI language %s: see %s\n", language, GuiState::GUI_LANGUAGE_FOLDER);
        return 1;
    }
    if (mode < 0) {
        mode = initialMode();
    }
    if (mode < 0) {
        printf("sceneEditor: no graphics mode with 4, 8, 16 or 24 bits per pixel\n");
        return 1;
    }
    // svgalib saves the terminal mode at its first vga_setmode() and puts it back in TEXT.
    vga_setmode(TEXT);
    input.open();
    if (!modes.enter(mode) || !surface.attach()) {
        input.close();
        modes.leave();
        printf("sceneEditor: cannot use mode %s\n", VgaModes::name(mode));
        return 1;
    }
    input.attach(surface);
    int status = 1;
    // Linux port note: the VSDKFatalException of the model are java::Exceptions; caught so that text mode comes back.
    const char *failure = nullptr;
    java::String message;
    try {
        FramebufferSceneEditor editor(surface, input, language);
        status = editor.run();
    } catch (const java::Exception &exception) {
        message = exception.getMessage();
        failure = message.c_str();
    }
    input.close();
    modes.leave();
    if (failure) {
        printf("sceneEditor: %s\n", failure);
    }
    return status;
}

int
SceneEditor::start(int argc, char *argv[]) {
    vga_setmousesupport(1);
    vga_init();
    SceneEditor *editor = new (std::nothrow) SceneEditor();
    if (!editor) {
        printf("sceneEditor: out of memory\n");
        return 1;
    }
    int status = editor->run(argc, argv);
    delete editor;
    return status;
}

int
main(int argc, char *argv[]) {
    return SceneEditor::start(argc, argv);
}
