#ifndef __SVGA_PRIMITIVES__
#define __SVGA_PRIMITIVES__
#include <stdint.h>

// The drawing operations of the SVGA modes, as the function pointers Svga calls. The frame buffer set, which Svga
// starts with, writes the pixels with the CPU through the driver; Svga::setAccelerated() installs a driver's own set
// (its 2D engine) in its place and puts the frame buffer one back. Callers pass rectangles inside the mode, with
// colours packed as the mode stores them.
class SvgaPrimitives {
public:
    const char *name;
    void (*plot)(unsigned x, unsigned y, unsigned bytesPerPixel, uint32_t color);
    void (*span)(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count);
    void (*fill)(unsigned x, unsigned y, unsigned width, unsigned height, unsigned bytesPerPixel, uint32_t color);
    // A copy of a rectangle of the screen onto another one, which may overlap it. False when the set cannot copy on
    // the screen (the frame buffer one: reading video memory is slow), so that the caller writes the destination.
    bool (*copy)(unsigned fromX, unsigned fromY, unsigned toX, unsigned toY, unsigned width, unsigned height);
    // Returns once everything drawn is in video memory.
    void (*sync)();
};

#endif
