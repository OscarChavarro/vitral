#include "drivers/video/svga/Svga.h"
#include "drivers/video/bochsVBE/BochsVbe.h"
#include "drivers/video/bootFramebuffer/BootFramebuffer.h"
#include "kernel/KernelLog.h"
#include "kernel/PhysicalMemory.h"
#include <stddef.h>

SvgaDriver *Svga::driver = nullptr;
bool Svga::on = false;

void
Svga::init() {
    if (driver) {
        return;
    }
    driver = BochsVbe::probe();
    if (!driver) {
        driver = BootFramebuffer::probe();
    }
    uint64_t ramMb = PhysicalMemory::totalFrames() * PhysicalMemory::FRAME >> 20;
    if (driver && ramMb < MIN_RAM_MB) {
        kprintf("Video: SVGA disabled, %u MB of RAM (needs %u MB): VGA modes only\n", (uint32_t)ramMb, MIN_RAM_MB);
        delete driver;
        driver = nullptr;
    }
}

bool
Svga::enter(unsigned width, unsigned height, unsigned bytesPerPixel) {
    if (!driver || !driver->setMode(width, height, bytesPerPixel)) {
        return false;
    }
    on = true;
    return true;
}

void
Svga::leave() {
    if (!on) {
        return;
    }
    driver->leaveMode();
    on = false;
}

void
Svga::blit(const uint8_t *pixels, unsigned width, unsigned height, unsigned bytesPerPixel) {
    if (!on) {
        return;
    }
    unsigned row = width * bytesPerPixel;
    for (unsigned y = 0; y < height; y++) {
        const uint8_t *src = pixels + (size_t)y * row;
        for (unsigned x = 0; x < width; x++) {
            uint32_t color = 0;
            for (unsigned i = 0; i < bytesPerPixel; i++) {
                color |= (uint32_t)src[x * bytesPerPixel + i] << (8 * i);
            }
            driver->writePixel(x, y, bytesPerPixel, color);
        }
    }
}

void
Svga::plot(int x, int y, unsigned bytesPerPixel, uint32_t color) {
    if (!on) {
        return;
    }
    driver->writePixel((unsigned)x, (unsigned)y, bytesPerPixel, color);
}
