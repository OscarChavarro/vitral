#include <new>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <vga.h>

#include "java/lang/String.h"
#include "programs/sceneEditor/SceneEditor.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferEvent.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferMenuReader.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"

static const int SUPPORTED_DEPTHS[] = {1, 4, 8, 16, 24};
static const int PREFERRED_MODES[] = {G640x480x256, G800x600x256, G640x480x16, G1024x768x256};
static const char *const DEFAULT_LANGUAGE = "english";

SceneEditor::SceneEditor()
    : modes(false), theme(FramebufferTheme::monochrome()), workX(0), workY(0), workWidth(0), workHeight(0),
      guiPath{}, choice{} {
    modes.keepDepths(SUPPORTED_DEPTHS, (int)(sizeof(SUPPORTED_DEPTHS) / sizeof(SUPPORTED_DEPTHS[0])));
}

bool
SceneEditor::loadGui(const char *path) {
    FILE *file = fopen(path, "rb");
    if (!file) {
        printf("sceneEditor: cannot open %s\n", path);
        return false;
    }
    char *json = nullptr;
    long size = -1;
    if (fseek(file, 0, SEEK_END) == 0) {
        size = ftell(file);
    }
    if (size >= 0 && fseek(file, 0, SEEK_SET) == 0) {
        json = new (std::nothrow) char[(size_t)size + 1];
    }
    bool read = json && fread(json, 1, (size_t)size, file) == (size_t)size;
    fclose(file);
    if (!read) {
        printf("sceneEditor: cannot read %s\n", path);
        delete[] json;
        return false;
    }
    json[size] = 0;
    bool loaded = FramebufferMenuReader::read(java::String(json), widget);
    delete[] json;
    if (!loaded) {
        printf("sceneEditor: no menu bar in %s\n", path);
        return false;
    }
    menuBar.setMenu(widget.getMenubar());
    return true;
}

int
SceneEditor::initialMode() const {
    for (int mode : PREFERRED_MODES) {
        if (modes.indexOf(mode) >= 0) {
            return mode;
        }
    }
    return modes.count() > 0 ? modes.at(0) : -1;
}

// Attaches the framebuffer and the input to the mode just set, lays the screen out and draws everything.
bool
SceneEditor::show() {
    if (!surface.attach()) {
        return false;
    }
    theme = FramebufferTheme::forSurface(surface);
    uint32_t colors[16];
    int count = theme.colors(colors, 16);
    for (int i = 0; i < count; i++) {
        surface.native(colors[i]);
    }
    // Full screen: the menu bar along the top, the work area below it.
    int barHeight = menuBar.layout(surface, theme, 0, 0, surface.width());
    workX = 0;
    workY = barHeight;
    workWidth = surface.width();
    workHeight = surface.height() - barHeight;
    input.attach(surface);
    redraw();
    return true;
}

void
SceneEditor::redraw() {
    surface.beginDrawing();
    menuBar.draw();
    drawStatus();
    surface.endDrawing();
}

// The mode, the keys and the last chosen item, written on the work area.
void
SceneEditor::drawStatus() {
    if (workWidth <= 0 || workHeight <= 0) {
        return;
    }
    char lines[5][STATUS_LENGTH];
    int mode = modes.current();
    snprintf(lines[0], STATUS_LENGTH, "Mode %s: %dx%d, %d bits per pixel", VgaModes::name(mode), surface.width(),
             surface.height(), surface.bitsPerPixel());
    snprintf(lines[1], STATUS_LENGTH, "Menus: %s", guiPath);
    snprintf(lines[2], STATUS_LENGTH, "F10, arrows, Enter: menus   Esc: close / quit");
    snprintf(lines[3], STATUS_LENGTH, "r / R: next / previous mode   %s",
             input.hasPointer() ? "Mouse: on" : "No mouse");
    snprintf(lines[4], STATUS_LENGTH, "%s", choice[0] ? choice : "No item chosen yet");

    surface.beginDrawing();
    surface.fillRectangle(workX, workY, workWidth, workHeight, theme.face);
    surface.setClip(workX, workY, workWidth, workHeight);
    int inset = surface.fontWidth();
    int lineHeight = surface.fontHeight() + 2;
    for (int i = 0; i < 5; i++) {
        surface.text(workX + inset, workY + inset / 2 + i * lineHeight, lines[i], theme.text);
    }
    surface.resetClip();
    surface.endDrawing();
}

void
SceneEditor::report(WidgetMenuItem *item) {
    if (!item) {
        return;
    }
    snprintf(choice, STATUS_LENGTH, "Chosen: %s [%s], not executed", item->getName().c_str(),
             item->getCommandName().c_str());
    drawStatus();
}

void
SceneEditor::step(int direction) {
    input.detach();
    VgaModes::Change change = modes.step(direction);
    if (change == VgaModes::Change::Failed) {
        snprintf(choice, STATUS_LENGTH, "Could not change the graphics mode");
    }
    if (!show()) {
        // The new mode cannot be drawn on: go back to the one before.
        modes.step(-direction);
        show();
    }
}

int
SceneEditor::run(int argc, char *argv[]) {
    const char *language = DEFAULT_LANGUAGE;
    int mode = -1;
    for (int i = 1; i < argc; i++) {
        int asked = VgaModes::lookup(argv[i]);
        if (asked > 0 && modes.indexOf(asked) >= 0) {
            mode = asked;
        } else if (asked > 0 || strcmp(argv[i], "-h") == 0) {
            printf("usage: sceneEditor [english | spanish | FILE.json] [MODE]\n");
            printf("MODE: one of the graphics modes with 1, 4, 8, 16 or 24 bits per pixel:\n");
            modes.print("Modes");
            return 1;
        } else {
            language = argv[i];
        }
    }
    if (strchr(language, '/') || strstr(language, ".json")) {
        snprintf(guiPath, STATUS_LENGTH, "%s", language);
    } else {
        snprintf(guiPath, STATUS_LENGTH, "%s/%s.json", SCENE_EDITOR_GUI_DIR, language);
    }
    if (!loadGui(guiPath)) {
        return 1;
    }
    if (mode < 0) {
        mode = initialMode();
    }
    if (mode < 0) {
        printf("sceneEditor: no graphics mode with 1, 4, 8, 16 or 24 bits per pixel\n");
        return 1;
    }
    // svgalib saves the terminal mode at its first vga_setmode() and puts it back in TEXT.
    vga_setmode(TEXT);
    input.open();
    if (!modes.enter(mode) || !show()) {
        input.close();
        modes.leave();
        printf("sceneEditor: cannot use mode %s\n", VgaModes::name(mode));
        return 1;
    }
    FramebufferEvent event;
    while (input.waitEvent(event)) {
        if (event.type != FramebufferEvent::Type::KeyPressed) {
            menuBar.mouseEvent(event);
            report(menuBar.takeChosen());
            continue;
        }
        if (menuBar.keyPressed(event.key)) {
            report(menuBar.takeChosen());
            continue;
        }
        int key = event.key.keycode;
        if (key == KeyEvent::KEY_ESC) {
            break;
        }
        if (key == KeyEvent::KEY_r || key == KeyEvent::KEY_R) {
            step(key == KeyEvent::KEY_r ? 1 : -1);
        }
    }
    input.close();
    modes.leave();
    return 0;
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
