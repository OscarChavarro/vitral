#ifndef VITRAL_SVGALIB_INPUT_H
#define VITRAL_SVGALIB_INPUT_H

#include "vsdk/toolkit/gui/framebuffer/FramebufferEvent.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferInput.h"

class SvgalibFramebuffer;

// Keyboard (the terminal bytes of vga_getkey(), with ANSI escape sequences) and svgalib mouse input, translated to
// Vitral key and mouse events. The mouse pointer is drawn by the SvgalibFramebuffer.
class SvgalibInput : public FramebufferInput {
public:
    SvgalibInput();
    ~SvgalibInput();
    SvgalibInput(const SvgalibInput &) = delete;
    SvgalibInput &operator=(const SvgalibInput &) = delete;

    // Uses the mouse that vga_init() opened when mouse support was requested before it.
    void open();
    void close();
    // Fits the mouse to a newly attached framebuffer, also the scale of the absolute pointers of Linux, places the
    // pointer and shows it.
    void attach(SvgalibFramebuffer &surface);
    // Removes the pointer before the mode changes, and puts that scale back.
    void detach();

    bool waitEvent(FramebufferEvent &event) override;
    bool hasPointer() const override { return connected; }

private:
    static constexpr int QUEUE_SIZE = 8;

    bool connected;
    bool attached;
    SvgalibFramebuffer *surface;
    int buttons;
    int lastX;
    int lastY;
    int savedScaleWidth;
    int savedScaleHeight;
    FramebufferEvent queue[QUEUE_SIZE];
    int queued;

    void fitAbsolutePointer(int width, int height, int &startX, int &startY);
    void restoreAbsolutePointer();
    bool readKey(KeyEvent &event);
    void readMouse();
    void push(const FramebufferEvent &event);
    static int keyFor(int c);
    static void modifiedKey(int modifiers, int code, KeyEvent &event);
    static int buttonMask(int button);
};

#endif
