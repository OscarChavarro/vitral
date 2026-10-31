#ifndef VITRAL_XLIB_INPUT_H
#define VITRAL_XLIB_INPUT_H

#include <X11/Xlib.h>
#include "vsdk/toolkit/gui/framebuffer/FramebufferEvent.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferInput.h"

class XlibFramebuffer;

// Keyboard and pointer input of the window of an XlibFramebuffer, translated to Vitral key and mouse events as
// XtSystem does for Xt. Exposures are repaired from the back buffer without reporting them; a new window size is
// reported as a Resized event once the back buffer has that size. Consecutive pointer motions are merged, so that
// a slow redraw does not fall behind the pointer, and the key releases of auto repeat are dropped. The window
// manager's close request ends the input: waitEvent() returns false. Presses of the same button close in time and
// place count as multiple clicks (the clicks of the press and of its release).
class XlibInput : public FramebufferInput {
public:
    explicit XlibInput(XlibFramebuffer &surface);
    XlibInput(const XlibInput &) = delete;
    XlibInput &operator=(const XlibInput &) = delete;

    bool waitEvent(FramebufferEvent &event) override;
    bool hasPointer() const override { return true; }
    bool warpPointer(int x, int y) override;
    // True once the window manager asked to close the window.
    bool isClosed() const { return closed; }

private:
    XlibFramebuffer &surface;
    Atom deleteWindow;
    Atom protocols;
    bool closed;
    bool warped;
    int warpX;
    int warpY;
    unsigned int clickButton;
    Time clickTime;
    int clickX;
    int clickY;
    int clicks;

    bool translate(XEvent &native, FramebufferEvent &event);
    bool isAutoRepeat(const XEvent &release);
    static KeyEvent keyEvent(XKeyEvent &native);
    static int modifiers(unsigned int state);
    static int buttonMask(unsigned int button);
};

#endif
