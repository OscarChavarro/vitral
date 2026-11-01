#ifndef __SCENE_EDITOR__
#define __SCENE_EDITOR__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferMenuBar.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferTheme.h"
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiInput.h"
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiModes.h"
#include "vsdk/toolkit/gui/widget/Widget.h"

class WidgetMenuItem;

// The menus of the Scene Editor on a BGI framebuffer, for real mode DOS: the program of the cpp minimal kernel
// (x86_64_smp_kernel/src/main/programs/sceneEditor) on the BGI backend of turboc/bgi instead of the svgalib one. It
// reads the menu bar of a Vitral GUI definition and lets the user browse it with the mouse and the keyboard; chosen
// items are reported, not executed. r and R step through the graphics modes with 1, 4, 8, 16 or 24 bits per pixel;
// Escape closes a menu, then quits.
class SceneEditor {
public:
    enum { STATUS_LENGTH = 160 };

    static int start(int argc, char *argv[]);

private:
    BgiModes modes;
    Widget widget;
    FramebufferMenuBar menuBar;
    FramebufferTheme theme;
    BgiFramebuffer surface;
    BgiInput input;
    int workX;
    int workY;
    int workWidth;
    int workHeight;
    char guiPath[STATUS_LENGTH];
    char choice[STATUS_LENGTH];

    SceneEditor();
    int run(int argc, char *argv[]);
    BOOLEAN loadGui(const char *path);
    int initialMode() const;
    BOOLEAN show();
    void redraw();
    void drawStatus();
    void step(int direction);
    void report(WidgetMenuItem *item);
};

#endif
