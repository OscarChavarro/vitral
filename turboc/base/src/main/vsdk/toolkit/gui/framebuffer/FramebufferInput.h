#ifndef VITRAL_FRAMEBUFFER_INPUT_H
#define VITRAL_FRAMEBUFFER_INPUT_H

#include "vsdk/toolkit/common/VSDK.h"

class FramebufferEvent;

// Keyboard and pointer input of a framebuffer backend.
class FramebufferInput {
public:
    virtual ~FramebufferInput() {}
    // Waits for the next event. Returns false only when no more input can arrive.
    virtual BOOLEAN waitEvent(FramebufferEvent &event) = 0;
    virtual BOOLEAN hasPointer() const = 0;
    // Moves the pointer to a surface position, without reporting a MouseMoved event for it. Backends that cannot
    // place the pointer ignore it and return false.
    virtual BOOLEAN warpPointer(int, int) { return FALSE; }
};

#endif
