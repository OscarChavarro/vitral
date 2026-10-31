#ifndef __SCENE_EDITOR__
#define __SCENE_EDITOR__

#include "lib/vgaModes/VgaModes.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferMenuBar.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferTheme.h"
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibInput.h"
#include "vsdk/toolkit/gui/widget/Widget.h"

class WidgetMenuItem;

// The menus of the Scene Editor on a svgalib framebuffer. It reads the menu bar of a Vitral GUI definition and lets
// the user browse it with the mouse and the keyboard; chosen items are reported, not executed. r and R step through
// the graphics modes with 1, 4, 8, 16 or 24 bits per pixel; Escape closes a menu, then quits.
class SceneEditor {
public:
    static constexpr int STATUS_LENGTH = 160;

    static int start(int argc, char *argv[]);

private:
    VgaModes modes;
    Widget widget;
    FramebufferMenuBar menuBar;
    FramebufferTheme theme;
    SvgalibFramebuffer surface;
    SvgalibInput input;
    int workX;
    int workY;
    int workWidth;
    int workHeight;
    char guiPath[STATUS_LENGTH];
    char choice[STATUS_LENGTH];

    SceneEditor();
    int run(int argc, char *argv[]);
    bool loadGui(const char *path);
    int initialMode() const;
    bool show();
    void redraw();
    void drawStatus();
    void step(int direction);
    void report(WidgetMenuItem *item);
};

#endif
