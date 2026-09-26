#ifndef __XT_SYSTEM__
#define __XT_SYSTEM__

#include <X11/Xlib.h>

#include "java/lang/String.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"

class ColorRgb;
class RGBAImageUncompressed;

/**
Services of the X Window System (Xlib / Xt) for vitral applications, as
`AwtSystem` offers the AWT ones in the Java toolkit: conversion of the input
events to vitral ones and rasterization of text labels.

Events:
- Mouse modifiers follow the AWT extended masks the interaction techniques
  expect (`MouseEvent::BUTTON1_DOWN_MASK`...). As in AWT, the mask of a press
  includes the pressed button and the one of a release excludes it (X
  reports the state previous to the event).
- Keys are identified by their character; Ctrl+letter chords keep the
  letter in `KeyEvent::keycode` (`KEY_a`..`KEY_z`, or `KEY_A`..`KEY_Z` with
  Shift) while the character is the control one, so the chord is not taken
  as the plain letter command.
*/
class XtSystem {
public:
    /** AWT extended masks not named by `MouseEvent` */
    static const int SHIFT_DOWN_MASK = 64;
    static const int ALT_DOWN_MASK = 512;

    /**
    @param event X ButtonPress, ButtonRelease, MotionNotify, EnterNotify or
    LeaveNotify
    @return the vitral event, with coordinates of the window of the event
    */
    static MouseEvent xt2vsdkMouseEvent(const XEvent& event);

    /**
    @param event X ButtonPress of the buttons 4 to 7
    @return the vitral wheel event (its clicks are the wheel rotation,
    negative upwards), or one with zero clicks for other buttons
    */
    static MouseEvent xt2vsdkWheelEvent(const XEvent& event);

    /**
    @param event X ButtonPress or ButtonRelease
    @return true if the button is one of the wheel ones
    */
    static bool isWheelButton(const XEvent& event);

    /**
    @param event X KeyPress or KeyRelease
    @return the vitral event
    */
    static KeyEvent xt2vsdkKeyEvent(XKeyEvent& event);

    /**
    Maps a key already looked up (i.e. a synthetic one, delivered to the
    application by an automated agent).
    @param keysym symbol of the key with the modifiers applied
    @param baseKeysym symbol of the key without modifiers (its level 0)
    @param text characters of the key, as `XLookupString` gives them
    @param count number of characters of the key
    @param state X modifiers mask (`ShiftMask`, `ControlMask`...)
    @return the vitral event
    */
    static KeyEvent xt2vsdkKeyEvent(KeySym keysym, KeySym baseKeysym,
                                    const char* text, int count,
                                    unsigned int state);

    /**
    Rasterizes a text with the UTF-8 font sets of Xlib (Helvetica, or the
    closest font of that size), as `AwtSystem.calculateLabelImage`.
    @param display connection whose fonts are used
    @param label text of the label
    @param color color of the text
    @param fontSize size of the font, in pixels
    @return a new image of the size of the text: the color with the text
    coverage as opacity (a transparent pixel for an empty text)
    */
    static RGBAImageUncompressed* calculateLabelImage(
        Display* display, const java::String& label, const ColorRgb& color,
        int fontSize);

    /**
    Frees the font sets created for a display by `calculateLabelImage`.
    Call it before closing the display.
    */
    static void releaseResources(Display* display);

private:
    static int buttonMaskFor(unsigned int button);
    static int stateToModifiers(unsigned int state);
    XtSystem() {}
};

#endif
