#ifndef __GLUT_SYSTEM__
#define __GLUT_SYSTEM__

#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"

class ColorRgb;
class RGBAImageUncompressed;

class GlutSystem {
public:
    static KeyEvent glut2vsdkKeyEvent(unsigned char glutKey, int modifiers);
    static KeyEvent glut2vsdkSpecialKeyEvent(int glutSpecialKey, int modifiers);
    static MouseEvent glut2vsdkMouseEvent(int button, int state, int x, int y);
    static MouseEvent glut2vsdkMotionEvent(int x, int y);
    static MouseEvent glut2vsdkWheelEvent(int wheel, int direction, int x, int y);

    /**
    GLUT does not tell which buttons are down during a motion: this keeps
    track of them, from the events given to the mouse callback.
    @param button button of the GLUT mouse callback
    @param state `GLUT_DOWN` or `GLUT_UP`
    */
    static void updateMouseButtonsState(int button, int state);

    /**
    @return the `MouseEvent::BUTTON*_DOWN_MASK` bits of the buttons that are
    down, as registered with `updateMouseButtonsState`
    */
    static int getMouseButtonsDownMask();

    /**
    GLUT implementations without a wheel callback report the wheel as the
    buttons 3 (up) and 4 (down) of the mouse callback.
    @param outDirection +1 for up, -1 for down
    @return true if the button is a wheel step
    */
    static bool isWheelButton(int button, int* outDirection);

    /**
    Rasterizes a text label as an image: the text color with an alpha
    coverage, over a transparent background (the counterpart of
    `AwtSystem.calculateLabelImage`). With X11, the core font closest to
    Helvetica of the given pixel size is used; without it, or if no font is
    found, a transparent 1x1 image is returned.
    @return a new image, owned by the caller
    */
    static RGBAImageUncompressed* calculateLabelImage(
        const char* text, const ColorRgb& color, int fontSize);

    /**
    Releases the X11 resources used by `calculateLabelImage`.
    */
    static void releaseResources();

private:
    static int mouseButtonsDownMask;

    GlutSystem();
    ~GlutSystem();
};

#endif
