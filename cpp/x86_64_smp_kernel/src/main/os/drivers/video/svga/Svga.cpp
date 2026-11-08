#include "drivers/video/svga/Svga.h"
#include "drivers/video/bochsVBE/BochsVbe.h"
#include "drivers/video/bootFramebuffer/BootFramebuffer.h"
#include "drivers/video/nvidiaPreTesla/NvidiaPreTesla.h"
#include "kernel/KernelLog.h"
#include "kernel/Paging.h"
#include "kernel/PhysicalMemory.h"
#include <stddef.h>

SvgaDriver *Svga::driver = nullptr;
bool Svga::on = false;
const SvgaPrimitives Svga::FRAMEBUFFER = {
    "framebuffer", &Svga::framebufferPlot, &Svga::framebufferSpan, &Svga::framebufferFill, &Svga::framebufferCopy,
    &Svga::framebufferSync,
};
const SvgaPrimitives *Svga::primitives = &Svga::FRAMEBUFFER;
SpinLock Svga::lock;

void
Svga::init() {
    if (driver) {
        return;
    }
    driver = BochsVbe::probe();
    if (!driver) {
        driver = NvidiaPreTesla::probe();
    }
    if (!driver) {
        driver = BootFramebuffer::probe();
    }
    uint64_t ramMb = PhysicalMemory::totalFrames() * PhysicalMemory::FRAME >> 20;
    if (driver && ramMb < MIN_RAM_MB) {
        kprintf("Video: SVGA disabled, %u MB of RAM (needs %u MB): VGA modes only\n", (uint32_t)ramMb, MIN_RAM_MB);
        delete driver;
        driver = nullptr;
    }
    if (driver) {
        uint64_t frame = (uint64_t)(uintptr_t)driver->frameBuffer();
        bool combining = Paging::mapWriteCombining(frame, driver->videoMemoryBytes());
        kprintf("Video: frame buffer memory type: %s\n", combining ? "write-combining" : "uncached");
    }
}

bool
Svga::enter(unsigned width, unsigned height, unsigned bytesPerPixel) {
    Exclusive guard;
    if (!driver || !driver->setMode(width, height, bytesPerPixel)) {
        return false;
    }
    on = true;
    return true;
}

void
Svga::leave() {
    Exclusive guard;
    if (!on) {
        return;
    }
    primitives->sync();
    driver->leaveMode();
    on = false;
}

void
Svga::blit(const uint8_t *pixels, unsigned width, unsigned height, unsigned bytesPerPixel) {
    Exclusive guard;
    if (!on) {
        return;
    }
    unsigned row = width * bytesPerPixel;
    for (unsigned y = 0; y < height; y++) {
        primitives->span(0, y, bytesPerPixel, pixels + (size_t)y * row, width);
    }
}

void
Svga::plot(int x, int y, unsigned bytesPerPixel, uint32_t color) {
    Exclusive guard;
    if (!on) {
        return;
    }
    primitives->plot((unsigned)x, (unsigned)y, bytesPerPixel, color);
}

void
Svga::span(int x, int y, unsigned bytesPerPixel, const uint8_t *pixels, int count) {
    Exclusive guard;
    if (!on) {
        return;
    }
    primitives->span((unsigned)x, (unsigned)y, bytesPerPixel, pixels, (unsigned)count);
}

void
Svga::fill(int x, int y, int width, int height, unsigned bytesPerPixel, uint32_t color) {
    Exclusive guard;
    if (!on || x < 0 || y < 0 || width <= 0 || height <= 0) {
        return;
    }
    primitives->fill((unsigned)x, (unsigned)y, (unsigned)width, (unsigned)height, bytesPerPixel, color);
}

bool
Svga::copy(int fromX, int fromY, int toX, int toY, int width, int height) {
    Exclusive guard;
    if (!on || fromX < 0 || fromY < 0 || toX < 0 || toY < 0 || width <= 0 || height <= 0) {
        return false;
    }
    return primitives->copy((unsigned)fromX, (unsigned)fromY, (unsigned)toX, (unsigned)toY, (unsigned)width,
                            (unsigned)height);
}

void
Svga::sync() {
    Exclusive guard;
    primitives->sync();
}

bool
Svga::setAccelerated(bool accelerated) {
    Exclusive guard;
    if (!driver) {
        return false;
    }
    if (!accelerated) {
        if (primitives != &FRAMEBUFFER) {
            primitives->sync();
            primitives = &FRAMEBUFFER;
            driver->stopAcceleration();
        }
        return true;
    }
    if (primitives != &FRAMEBUFFER) {
        return true;
    }
    const SvgaPrimitives *engine = driver->startAcceleration();
    if (!engine) {
        return false;
    }
    primitives = engine;
    return true;
}

void
Svga::framebufferPlot(unsigned x, unsigned y, unsigned bytesPerPixel, uint32_t color) {
    driver->writePixel(x, y, bytesPerPixel, color);
}

void
Svga::framebufferSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count) {
    driver->writeSpan(x, y, bytesPerPixel, pixels, count);
}

void
Svga::framebufferFill(unsigned x, unsigned y, unsigned width, unsigned height, unsigned bytesPerPixel,
                      uint32_t color) {
    driver->fillRect(x, y, width, height, bytesPerPixel, color);
}

bool
Svga::framebufferCopy(unsigned, unsigned, unsigned, unsigned, unsigned, unsigned) {
    return false;
}

void
Svga::framebufferSync() {}
