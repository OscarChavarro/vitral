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
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"

static const int SUPPORTED_DEPTHS[] = {4, 8, 16, 24};
// Direct colour first: the raytraced views need more colours than a palette has.
static const int PREFERRED_MODES[] = {G1024x768x64K, G800x600x64K, G1024x768x16M, G800x600x16M, G640x480x64K,
                                      G640x480x16M, G1024x768x256, G800x600x256, G640x480x256, G640x480x16};
static const char *const DEFAULT_LANGUAGE = "english";

SceneEditor::SceneEditor() : modes(false), message() {
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
        FramebufferSceneEditor editor(surface, *this, language);
        status = editor.run();
    }
    input.close();
    modes.leave();
    return status;
}

bool
SceneEditor::isModeKey(const KeyEvent &key) {
    int others = KeyEvent::MASK_ALT | KeyEvent::MASK_LALT | KeyEvent::MASK_RALT | KeyEvent::MASK_ALTGR;
    return (key.modifierMask & KeyEvent::MASK_CTRL) != 0 && (key.modifierMask & others) == 0 &&
           (key.keycode == KeyEvent::KEY_r || key.keycode == KeyEvent::KEY_R);
}

bool
SceneEditor::waitEvent(FramebufferEvent &event) {
    if (!input.waitEvent(event)) {
        return false;
    }
    event.message = nullptr;
    if (event.type != FramebufferEvent::Type::KeyPressed || !isModeKey(event.key)) {
        return true;
    }
    // As in vitral_rasterizer: R goes to the next mode of the list, r to the previous one.
    if (!changeMode(event.key.keycode == KeyEvent::KEY_R ? 1 : -1)) {
        return false;
    }
    event = FramebufferEvent();
    event.type = FramebufferEvent::Type::Resized;
    event.message = message;
    return true;
}

// The pointer leaves the old mode before the surface describes the new one; if the new mode cannot be used, the
// previous one comes back. Returns false when neither can be shown.
bool
SceneEditor::changeMode(int step) {
    int previous = modes.current();
    int at = modes.indexOf(previous);
    int count = modes.count();
    int next = at < 0 || count == 0 ? -1 : modes.at(((at + step) % count + count) % count);
    if (next < 0 || next == previous) {
        snprintf(message, sizeof(message), "No other graphics mode than %s", VgaModes::name(previous));
        return true;
    }
    return selectMode(next);
}

bool
SceneEditor::selectMode(int next) {
    int previous = modes.current();
    if (next == previous) {
        snprintf(message, sizeof(message), "Already using %s", VgaModes::name(previous));
        return true;
    }
    input.detach();
    if (show(next)) {
        const VgaModes::Screen &screen = modes.screen();
        snprintf(message, sizeof(message), "Resolution %dx%d, %d bits per pixel (%s)", screen.width, screen.height,
                 screen.bitsPerPixel, VgaModes::name(screen.mode));
        return true;
    }
    if (!show(previous)) {
        return false;
    }
    snprintf(message, sizeof(message), "Mode %s not available, staying at %s", VgaModes::name(next),
             VgaModes::name(previous));
    return true;
}

void
SceneEditor::extendMenu(Widget &widget) {
    if (!widget.getMenubar() || modes.count() == 0) {
        return;
    }
    WidgetMenu *menu = new WidgetMenu(&widget);
    menu->setName(widget.getMessage("IDM_RESOLUTION"));
    for (int i = 0; i < modes.count(); i++) {
        const char *name = VgaModes::name(modes.at(i));
        WidgetMenuItem *item = new WidgetMenuItem(&widget);
        item->setName(name);
        item->setCommandName(java::String("IDC_RESOLUTION_") + name);
        menu->addChild(item);
    }
    WidgetMenu *parent = widget.getMenubar();
    for (int i = 0; i < parent->getChildren().size(); i++) {
        WidgetMenuElement *entry = parent->getChildren().get(i);
        if (entry->isMenu() && static_cast<WidgetMenu *>(entry)->getName().equals(widget.getMessage("IDM_VIEW_MENU"))) {
            parent = static_cast<WidgetMenu *>(entry);
            break;
        }
    }
    parent->addChild(menu);
}

bool
SceneEditor::executeMenuCommand(const char *command, const char *&status) {
    static const char prefix[] = "IDC_RESOLUTION_";
    if (strncmp(command, prefix, sizeof(prefix) - 1) != 0) {
        return false;
    }
    int mode = VgaModes::lookup(command + sizeof(prefix) - 1);
    if (modes.indexOf(mode) >= 0 && !selectMode(mode)) {
        snprintf(message, sizeof(message), "Unable to restore graphics mode");
    }
    status = message;
    return true;
}

bool
SceneEditor::show(int mode) {
    if ((modes.current() != mode && !modes.enter(mode)) || !surface.attach()) {
        return false;
    }
    input.attach(surface);
    return true;
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
