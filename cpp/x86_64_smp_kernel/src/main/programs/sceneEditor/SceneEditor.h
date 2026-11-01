#ifndef __SCENE_EDITOR__
#define __SCENE_EDITOR__

#include "lib/vgaModes/VgaModes.h"
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibInput.h"

// The Scene Editor of Vitral (FramebufferSceneEditor, from cpp/testsuite/ApplicationCases/SceneEditorApplication) on
// the svgalib backend of the framebuffer GUI, full screen in one graphics mode with 4, 8, 16 or 24 bits per pixel.
// The application finds the etc folder of Vitral at ../../../../etc, as in the repository: it runs from
// SCENE_EDITOR_APPLICATION_DIR, a folder of the root file system image placed like its sources are.
class SceneEditor {
public:
    static int start(int argc, char *argv[]);

private:
    VgaModes modes;
    SvgalibFramebuffer surface;
    SvgalibInput input;

    SceneEditor();
    int run(int argc, char *argv[]);
    int initialMode() const;
    void usage() const;
    static bool isKnownLanguage(const char *language);
};

#endif
