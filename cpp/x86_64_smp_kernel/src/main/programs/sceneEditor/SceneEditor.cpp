#include <new>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <vga.h>

#include "application/framebuffer/FramebufferSceneEditor.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "model/GuiState.h"
#include "programs/sceneEditor/SceneEditor.h"

static const int SUPPORTED_DEPTHS[] = {4, 8, 16, 24};
// Direct colour first: the raytraced views need more colours than a palette has.
static const int PREFERRED_MODES[] = {G1024x768x64K, G800x600x64K, G1024x768x16M, G800x600x16M, G640x480x64K,
                                      G640x480x16M, G1024x768x256, G800x600x256, G640x480x256, G640x480x16};
static const char *const DEFAULT_LANGUAGE = "english";

SceneEditor::SceneEditor() : modes(false) {
    modes.keepDepths(SUPPORTED_DEPTHS, (int)(sizeof(SUPPORTED_DEPTHS) / sizeof(SUPPORTED_DEPTHS[0])));
}

int
SceneEditor::initialMode() const {
    for (int mode : PREFERRED_MODES) {
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
    int status;
    {
        FramebufferSceneEditor editor(surface, input, language);
        status = editor.run();
    }
    input.close();
    modes.leave();
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
