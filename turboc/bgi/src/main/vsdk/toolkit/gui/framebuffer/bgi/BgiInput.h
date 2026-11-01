#ifndef VITRAL_BGI_INPUT_H
#define VITRAL_BGI_INPUT_H

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferEvent.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferInput.h"
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiPointer.h"

class BgiFramebuffer;

// Keyboard (the enhanced BIOS keyboard services of INT 16h) and Microsoft mouse driver (INT 33h) input, translated
// to Vitral key and mouse events, as SvgalibInput of the cpp port. The mouse pointer is drawn with BgiPointer.
class BgiInput : public FramebufferInput {
public:
    BgiInput();
    ~BgiInput();

    // Resets the mouse driver, when there is one.
    void open();
    void close();
    // Fits the mouse to a newly attached framebuffer, centers the pointer and shows it.
    void attach(BgiFramebuffer &surface);
    // Removes the pointer before the mode changes.
    void detach();

    virtual BOOLEAN waitEvent(FramebufferEvent &event);
    virtual BOOLEAN hasPointer() const { return connected; }

private:
    enum { QUEUE_SIZE = 8 };

    BOOLEAN connected;
    BOOLEAN attached;
    BgiFramebuffer *surface;
    BgiPointer cursor;
    int buttons;
    int lastX;
    int lastY;
    FramebufferEvent queue[QUEUE_SIZE];
    int queued;

    BgiInput(const BgiInput &other);
    BgiInput &operator=(const BgiInput &other);

    BOOLEAN readKey(KeyEvent &event);
    void readMouse();
    void push(const FramebufferEvent &event);
    static int keyFor(int c);
    static int extendedKey(int scan, int &modifiers, char &letter);
    static int buttonMask(int button);
};

#endif
