#ifndef VITRAL_SVGALIB_INPUT_H
#define VITRAL_SVGALIB_INPUT_H

#include "lib/mouseCursor/MouseCursor.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferEvent.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferInput.h"

class SvgalibFramebuffer;

// Keyboard (the terminal bytes of vga_getkey(), with ANSI escape sequences) and svgalib mouse input, translated to
// Vitral key and mouse events. The mouse pointer is drawn with MouseCursor.
class SvgalibInput : public FramebufferInput {
public:
    SvgalibInput();
    ~SvgalibInput();
    SvgalibInput(const SvgalibInput &) = delete;
    SvgalibInput &operator=(const SvgalibInput &) = delete;

    // Uses the mouse that vga_init() opened when mouse support was requested before it.
    void open();
    void close();
    // Fits the mouse to a newly attached framebuffer, centers the pointer and shows it.
    void attach(SvgalibFramebuffer &surface);
    // Removes the pointer before the mode changes.
    void detach();

    bool waitEvent(FramebufferEvent &event) override;
    bool hasPointer() const override { return connected; }

private:
    static constexpr int QUEUE_SIZE = 8;

    bool connected;
    bool attached;
    SvgalibFramebuffer *surface;
    MouseCursor cursor;
    int buttons;
    int lastX;
    int lastY;
    FramebufferEvent queue[QUEUE_SIZE];
    int queued;

    bool readKey(KeyEvent &event);
    void readMouse();
    void push(const FramebufferEvent &event);
    static int keyFor(int c);
    static int buttonMask(int button);
};

#endif
