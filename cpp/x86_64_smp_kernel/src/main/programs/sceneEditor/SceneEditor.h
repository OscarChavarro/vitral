#ifndef __SCENE_EDITOR__
#define __SCENE_EDITOR__

#include "lib/vgaModes/VgaModes.h"
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibInput.h"

// The Scene Editor of Vitral (FramebufferSceneEditor, from cpp/testsuite/ApplicationCases/SceneEditorApplication) on
// the svgalib backend of the framebuffer GUI, full screen in one graphics mode with 4, 8, 16 or 24 bits per pixel.
// The application finds the etc folder of Vitral at ../../../../etc, as in the repository: it runs from
// SCENE_EDITOR_APPLICATION_DIR, a folder of the root file system image placed like its sources are.
// The editor reads its events through this object, which takes Ctrl-R and Ctrl-r as the vitral_rasterizer takes R and
// r: the next or the previous graphics mode. It reports the change as a Resized event with a message that the editor
// shows in its status line.
class SceneEditor : public FramebufferInput {
public:
    static int start(int argc, char *argv[]);

    bool waitEvent(FramebufferEvent &event) override;
    void extendMenu(Widget &widget) override;
    bool executeMenuCommand(const char *command, const char *&status) override;
    bool hasPointer() const override { return input.hasPointer(); }
    bool warpPointer(int x, int y) override { return input.warpPointer(x, y); }

private:
    static constexpr int MESSAGE_SIZE = 96;

    VgaModes modes;
    SvgalibFramebuffer surface;
    SvgalibInput input;
    char message[MESSAGE_SIZE];

    SceneEditor();
    int run(int argc, char *argv[]);
    int initialMode() const;
    void usage() const;
    bool changeMode(int step);
    bool selectMode(int mode);
    bool show(int mode);
    static bool isModeKey(const KeyEvent &key);
    static bool isKnownLanguage(const char *language);
};

#endif
