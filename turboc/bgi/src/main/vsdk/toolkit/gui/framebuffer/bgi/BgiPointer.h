#ifndef VITRAL_BGI_POINTER_H
#define VITRAL_BGI_POINTER_H

#include "vsdk/toolkit/common/VSDK.h"

class Framebuffer;

// The mouse pointer drawn on a framebuffer, as the MouseCursor of the cpp minimal kernel: an arrow with a black
// outline, saved and drawn with native pixels, which the framebuffer hides while it draws. The mouse driver's own
// pointer is not used, as it only knows the standard VGA modes.
class BgiPointer {
public:
    enum { WIDTH = 12, HEIGHT = 19 };

    BgiPointer();

    void use(Framebuffer *surface, unsigned long fill, unsigned long outline);
    void moveTo(int x, int y);
    void show();
    void hide();
    BOOLEAN shown() const { return visible; }

private:
    Framebuffer *surface;
    unsigned long fillColor;
    unsigned long outlineColor;
    int posX;
    int posY;
    BOOLEAN visible;
    int savedX;
    int savedY;
    unsigned long saved[WIDTH * HEIGHT];

    void draw();
    void restore();
};

#endif
