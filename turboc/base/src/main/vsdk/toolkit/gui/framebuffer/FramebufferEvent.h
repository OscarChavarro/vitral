#ifndef VITRAL_FRAMEBUFFER_EVENT_H
#define VITRAL_FRAMEBUFFER_EVENT_H

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"

// One input event, already translated to Vitral's key and mouse events. Mouse coordinates are surface pixels;
// for MousePressed and MouseReleased the button is the one that changed, and the modifiers hold the buttons down.
// MouseWheel events carry the rotation in the clicks of the mouse event: negative away from the user, as in AWT.
// Resized means that the surface changed its size (a window backend whose window was resized): the application
// lays itself out again and redraws everything. Backends without those devices never report them.
// turboc port note: Type is a plain enum of the class (FramebufferEvent::KeyPressed), as the cpp port's enum class
// does not exist before C++11.
class FramebufferEvent {
public:
    enum Type { NoEvent, KeyPressed, KeyReleased, MouseMoved, MousePressed, MouseReleased, MouseWheel, Resized };

    FramebufferEvent() : type(NoEvent) {}

    BOOLEAN isKey() const { return type == KeyPressed || type == KeyReleased; }
    BOOLEAN isMouse() const {
        return type == MouseMoved || type == MousePressed || type == MouseReleased || type == MouseWheel;
    }

    Type type;
    KeyEvent key;
    MouseEvent mouse;
};

#endif
