#ifndef VITRAL_FRAMEBUFFER_INPUT_H
#define VITRAL_FRAMEBUFFER_INPUT_H

class FramebufferEvent;

// Keyboard and pointer input of a framebuffer backend.
class FramebufferInput {
public:
    virtual ~FramebufferInput() {}
    // Waits for the next event. Returns false only when no more input can arrive.
    virtual bool waitEvent(FramebufferEvent &event) = 0;
    virtual bool hasPointer() const = 0;
};

#endif
