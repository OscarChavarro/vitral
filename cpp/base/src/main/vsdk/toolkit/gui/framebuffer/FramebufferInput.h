#ifndef __VITRAL_FRAMEBUFFER_INPUT__
#define __VITRAL_FRAMEBUFFER_INPUT__

class FramebufferEvent;
class Widget;

// Keyboard and pointer input of a framebuffer backend.
class FramebufferInput {
public:
    virtual ~FramebufferInput() {}
    // Waits for the next event. Returns false only when no more input can arrive.
    virtual bool waitEvent(FramebufferEvent &event) = 0;
    // Waits for the next event at most a time, for animations: when none arrives in time, the event is a NoEvent
    // one. Returns false only when no more input can arrive. Backends without timed waits block as waitEvent().
    virtual bool waitEventWithTimeout(FramebufferEvent &event, int timeoutMilliseconds) {
        (void)timeoutMilliseconds;
        return waitEvent(event);
    }
    // True if events are waiting to be read, so the application can process them all before drawing a frame
    // that would present only the state after the first one. Backends that cannot tell return false.
    virtual bool hasPendingEvent() { return false; }
    virtual bool hasPointer() const = 0;
    virtual void extendMenu(Widget &) {}
    virtual bool executeMenuCommand(const char *, const char *&) { return false; }
    // Moves the pointer to a surface position, without reporting a MouseMoved event for it. Backends that cannot
    // place the pointer ignore it and return false.
    virtual bool warpPointer(int, int) { return false; }
};

#endif
