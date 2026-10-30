#ifndef __SVGA__
#define __SVGA__
#include <stdint.h>
#include "drivers/video/svga/SvgaDriver.h"

class Svga {
public:
    static constexpr uint32_t MIN_RAM_MB = 128;
    static void init();
    static bool present() { return driver != nullptr; }
    static const char *name() { return driver ? driver->name() : "none"; }
    static uint32_t videoMemoryBytes() { return driver ? driver->videoMemoryBytes() : 0; }

    static bool canSet(unsigned width, unsigned height, unsigned bytesPerPixel) {
        return driver && driver->canSet(width, height, bytesPerPixel);
    }
    static bool enter(unsigned width, unsigned height, unsigned bytesPerPixel);
    static void leave();
    static bool active() { return on; }
    static bool supportsFramebufferText() { return driver && driver->supportsFramebufferText(); }
    static unsigned framebufferWidth() { return driver ? driver->framebufferWidth() : 0; }
    static unsigned framebufferHeight() { return driver ? driver->framebufferHeight() : 0; }

    static void blit(const uint8_t *pixels, unsigned width, unsigned height, unsigned bytesPerPixel);
    static void plot(int x, int y, unsigned bytesPerPixel, uint32_t color);

private:
    static SvgaDriver *driver;
    static bool on;
};

#endif
