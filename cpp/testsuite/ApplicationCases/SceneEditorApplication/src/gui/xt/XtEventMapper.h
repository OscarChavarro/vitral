#ifndef __XT_EVENT_MAPPER__
#define __XT_EVENT_MAPPER__

#include <X11/Xlib.h>

#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"

/**
Maps X11 input events to vitral ones, as `AwtSystem` and
`AwtKeyEventMapper` do for AWT in the Java application:

- Mouse modifiers follow the AWT extended masks the interaction techniques
  expect (`MouseEvent::BUTTON1_DOWN_MASK`...). As in AWT, the mask of a press
  includes the pressed button and the one of a release excludes it (X
  reports the state previous to the event).
- Keys are identified by their character; Ctrl+letter chords keep the
  letter in `KeyEvent::keycode` (`KEY_a`..`KEY_z`, or `KEY_A`..`KEY_Z` with
  Shift) while the character is the control one, so the chord is not taken
  as the plain letter command.
*/
class XtEventMapper {
public:
    /** AWT extended masks not named by `MouseEvent` */
    static const int SHIFT_DOWN_MASK = 64;
    static const int ALT_DOWN_MASK = 512;

    /**
    @param event X ButtonPress, ButtonRelease, MotionNotify or EnterNotify
    @return the vitral event, with coordinates of the window of the event
    */
    static MouseEvent toMouseEvent(const XEvent& event);

    /**
    @param event X ButtonPress of the buttons 4 to 7
    @return the vitral wheel event (its clicks are the wheel rotation, negative
    upwards), or one with zero clicks for other buttons
    */
    static MouseEvent toMouseWheelEvent(const XEvent& event);

    /**
    @param event X ButtonPress or ButtonRelease
    @return true if the button is one of the wheel ones
    */
    static bool isWheelButton(const XEvent& event);

    /**
    @param event X KeyPress or KeyRelease
    @return the vitral event
    */
    static KeyEvent toKeyEvent(XKeyEvent& event);

    /**
    Maps a key already looked up (i.e. a synthetic one, see
    `XtOpenGL4ApplicationController::injectKeyEvent`).
    @param keysym symbol of the key with the modifiers applied
    @param baseKeysym symbol of the key without modifiers (its level 0)
    @param text characters of the key, as `XLookupString` gives them
    @param count number of characters of the key
    @param state X modifiers mask (`ShiftMask`, `ControlMask`...)
    @return the vitral event
    */
    static KeyEvent toKeyEvent(KeySym keysym, KeySym baseKeysym,
                               const char* text, int count,
                               unsigned int state);

private:
    static int buttonMaskFor(unsigned int button);
    static int stateToModifiers(unsigned int state);
    XtEventMapper() {}
};

#endif
