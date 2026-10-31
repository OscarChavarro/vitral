#ifndef VITRAL_FRAMEBUFFER_EVENT_H
#define VITRAL_FRAMEBUFFER_EVENT_H

#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"

// One input event, already translated to Vitral's key and mouse events. Mouse coordinates are surface pixels;
// for MousePressed and MouseReleased the button is the one that changed, and the modifiers hold the buttons down.
class FramebufferEvent {
public:
    enum class Type { None, KeyPressed, MouseMoved, MousePressed, MouseReleased };

    FramebufferEvent() : type(Type::None) {}

    Type type;
    KeyEvent key;
    MouseEvent mouse;
};

#endif
