#ifndef __SVGA__
#define __SVGA__
#include <stdint.h>
#include "drivers/video/svga/SvgaDriver.h"
#include "drivers/video/svga/SvgaPrimitives.h"
#include "lib/vitralKernel/SpinLock.h"

// The SVGA driver of the machine and the drawing operations of its modes. Drawing goes through a set of function
// pointers (SvgaPrimitives): the frame buffer one, which writes with the CPU, unless setAccelerated() has installed
// the driver's 2D engine instead. Drawing, mode changes and the switch between the sets hold one lock.
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
    static void span(int x, int y, unsigned bytesPerPixel, const uint8_t *pixels, int count);
    // A rectangle inside the mode.
    static void fill(int x, int y, int width, int height, unsigned bytesPerPixel, uint32_t color);
    // False when the current primitives cannot copy on the screen: the caller writes the destination itself.
    static bool copy(int fromX, int fromY, int toX, int toY, int width, int height);
    static void sync();

    // Installs the driver's 2D engine primitives (true when it has them and they start) or puts the frame buffer
    // ones back.
    static bool setAccelerated(bool accelerated);
    static bool accelerated() { return primitives != &FRAMEBUFFER; }
    static const char *primitivesName() { return primitives->name; }

    // Holds off drawing, mode changes and the primitive switch: for a driver that uses its hardware on its own (a self
    // test). Never print on the screen (kprintf) while holding it.
    class Exclusive {
    public:
        Exclusive() : flags(lock.lock()) {}
        ~Exclusive() { lock.unlock(flags); }

    private:
        uint64_t flags;
    };

private:
    static SvgaDriver *driver;
    static bool on;
    static const SvgaPrimitives FRAMEBUFFER;
    static const SvgaPrimitives *primitives;
    static SpinLock lock;

    static void framebufferPlot(unsigned x, unsigned y, unsigned bytesPerPixel, uint32_t color);
    static void framebufferSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels,
                                unsigned count);
    static void framebufferFill(unsigned x, unsigned y, unsigned width, unsigned height, unsigned bytesPerPixel,
                                uint32_t color);
    static bool framebufferCopy(unsigned fromX, unsigned fromY, unsigned toX, unsigned toY, unsigned width,
                                unsigned height);
    static void framebufferSync();
};

#endif
