#include "drivers/video/bootFramebuffer/BootFramebuffer.h"
#include "boot/BootInfo.h"
#include "kernel/Paging.h"
#include "kernel/KernelLog.h"

SvgaDriver *
BootFramebuffer::probe() {
    const BootInfo &boot = BootInfo::get();
    uint64_t bytes = (uint64_t)boot.frameBufferPitch * boot.frameBufferHeight;
    if (boot.frameBufferType != 1 || (boot.frameBufferBytes != 3 && boot.frameBufferBytes != 4) ||
        boot.frameBufferAddress == 0 ||
        boot.frameBufferWidth == 0 || boot.frameBufferHeight == 0 ||
        boot.frameBufferPitch < boot.frameBufferWidth * boot.frameBufferBytes ||
        boot.frameBufferAddress + bytes > Paging::mappedBytes() || boot.frameBufferRedSize == 0 ||
        boot.frameBufferGreenSize == 0 || boot.frameBufferBlueSize == 0) {
        return nullptr;
    }
    kprintf("Video: boot framebuffer %ux%u, %u bytes per line at 0x%x%08x\n", boot.frameBufferWidth,
             boot.frameBufferHeight, boot.frameBufferPitch, (uint32_t)(boot.frameBufferAddress >> 32),
             (uint32_t)boot.frameBufferAddress);
    return new BootFramebuffer(boot.frameBufferAddress, boot.frameBufferPitch, boot.frameBufferWidth,
                               boot.frameBufferHeight, boot.frameBufferBytes, boot.frameBufferRedPosition,
                               boot.frameBufferRedSize, boot.frameBufferGreenPosition, boot.frameBufferGreenSize,
                               boot.frameBufferBluePosition, boot.frameBufferBlueSize);
}

bool
BootFramebuffer::canSet(unsigned newWidth, unsigned newHeight, unsigned newBytes) const {
    return newWidth == width && newHeight == height && newBytes == 3;
}

bool
BootFramebuffer::setMode(unsigned newWidth, unsigned newHeight, unsigned newBytes) {
    return canSet(newWidth, newHeight, newBytes);
}

void
BootFramebuffer::writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) {
    if (x >= width || y >= height || sourceBytesPerPixel != 3) {
        return;
    }
    uint32_t red = (color >> 16) & 0xFF;
    uint32_t green = (color >> 8) & 0xFF;
    uint32_t blue = color & 0xFF;
    uint32_t packed = ((red >> (8 - redSize)) << redPosition) |
                      ((green >> (8 - greenSize)) << greenPosition) |
                      ((blue >> (8 - blueSize)) << bluePosition);
    volatile uint8_t *dst = frame + (uint64_t)y * pitch + (uint64_t)x * pixelBytes;
    for (unsigned i = 0; i < pixelBytes; i++) {
        dst[i] = (uint8_t)(packed >> (8 * i));
    }
}
