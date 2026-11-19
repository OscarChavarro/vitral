#include "drivers/video/nvidiaPascal/NvidiaPascalSvga.h"
#include "drivers/video/nvidiaPascal/NvidiaPascal.h"
#include "kernel/KernelLog.h"

NvidiaPascalSvga::NvidiaPascalSvga(volatile uint8_t *frame)
    : surface(frame), pitch(pitchFor(CONSOLE_WIDTH)), width(0), height(0) {
    // The VGA default of the first 16 colours, the rest grey levels, until a program loads its own
    static const uint32_t EGA[16] = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xAA5500, 0xAAAAAA,
                                     0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    for (unsigned i = 0; i < 256; i++) {
        palette[i] = 0xFF000000 | (i < 16 ? EGA[i] : i * 0x010101u);
    }
}

SvgaDriver *
NvidiaPascalSvga::create() {
    int result = NvidiaPascal::start();
    if (result != 0) {
        kprintf("Video: NVIDIA Pascal display engine does not start (error %d): boot framebuffer kept\n", result);
        return nullptr;
    }
    volatile uint8_t *frame = NvidiaPascal::vram(SURFACE_VRAM, SURFACE_BYTES);
    if (!frame) {
        kprintf("Video: NVIDIA Pascal VRAM aperture unusable: boot framebuffer kept\n");
        return nullptr;
    }
    return new NvidiaPascalSvga(frame);
}

bool
NvidiaPascalSvga::canSet(unsigned newWidth, unsigned newHeight, unsigned bytesPerPixel) const {
    return bytesPerPixel >= 1 && bytesPerPixel <= 3 && newWidth > 0 && newHeight > 0 &&
           newWidth <= NvidiaPascal::rasterWidth() && newHeight <= NvidiaPascal::rasterHeight() &&
           (uint64_t)pitchFor(newWidth) * newHeight <= SURFACE_BYTES;
}

bool
NvidiaPascalSvga::setMode(unsigned newWidth, unsigned newHeight, unsigned bytesPerPixel) {
    if (!canSet(newWidth, newHeight, bytesPerPixel)) {
        return false;
    }
    uint32_t newPitch = pitchFor(newWidth);
    if (!NvidiaPascal::scanout(SURFACE_VRAM, newWidth, newHeight, newPitch)) {
        return false;
    }
    pitch = newPitch;
    width = newWidth;
    height = newHeight;
    return true;
}

uint32_t
NvidiaPascalSvga::convert(unsigned bytesPerPixel, uint32_t color) const {
    switch (bytesPerPixel) {
    case 1: return palette[color & 0xFF];
    case 2: {
        uint32_t red = (color >> 11) & 0x1F;
        uint32_t green = (color >> 5) & 0x3F;
        uint32_t blue = color & 0x1F;
        return 0xFF000000 | (red << 3 | red >> 2) << 16 | (green << 2 | green >> 4) << 8 | (blue << 3 | blue >> 2);
    }
    default: return 0xFF000000 | (color & 0xFFFFFF);
    }
}

void
NvidiaPascalSvga::writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) {
    if (x >= width || y >= height) {
        return;
    }
    *(volatile uint32_t *)(surface + (size_t)y * pitch + (size_t)x * 4) = convert(sourceBytesPerPixel, color);
}

void
NvidiaPascalSvga::writeSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count) {
    if (y >= height || x >= width) {
        return;
    }
    if (count > width - x) {
        count = width - x;
    }
    volatile uint32_t *row = (volatile uint32_t *)(surface + (size_t)y * pitch) + x;
    for (unsigned i = 0; i < count; i++) {
        const uint8_t *p = pixels + (size_t)i * bytesPerPixel;
        uint32_t color = p[0];
        if (bytesPerPixel > 1) {
            color |= (uint32_t)p[1] << 8;
        }
        if (bytesPerPixel > 2) {
            color |= (uint32_t)p[2] << 16;
        }
        row[i] = convert(bytesPerPixel, color);
    }
}

void
NvidiaPascalSvga::fillRect(unsigned x, unsigned y, unsigned rectWidth, unsigned rectHeight, unsigned bytesPerPixel,
                           uint32_t color) {
    if (x >= width || y >= height) {
        return;
    }
    if (rectWidth > width - x) {
        rectWidth = width - x;
    }
    if (rectHeight > height - y) {
        rectHeight = height - y;
    }
    uint32_t value = convert(bytesPerPixel, color);
    for (unsigned row = 0; row < rectHeight; row++) {
        volatile uint32_t *line = (volatile uint32_t *)(surface + (size_t)(y + row) * pitch) + x;
        for (unsigned column = 0; column < rectWidth; column++) {
            line[column] = value;
        }
    }
}

// The 6 bit values of the VGA DAC, widened to 8 bits
bool
NvidiaPascalSvga::loadPalette(const uint8_t *rgb) {
    for (unsigned i = 0; i < 256; i++) {
        uint32_t red = rgb[i * 3] & 0x3F;
        uint32_t green = rgb[i * 3 + 1] & 0x3F;
        uint32_t blue = rgb[i * 3 + 2] & 0x3F;
        palette[i] =
            0xFF000000 | (red << 2 | red >> 4) << 16 | (green << 2 | green >> 4) << 8 | (blue << 2 | blue >> 4);
    }
    return true;
}
