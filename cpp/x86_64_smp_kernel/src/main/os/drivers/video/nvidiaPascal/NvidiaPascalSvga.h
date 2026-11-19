#ifndef __NVIDIA_PASCAL_SVGA__
#define __NVIDIA_PASCAL_SVGA__
#include <stdint.h>
#include "drivers/video/svga/SvgaDriver.h"

// The SVGA modes of a Pascal card (NvidiaPascal): one A8R8G8B8 surface in VRAM, written by the CPU through BAR1 and
// scanned out by the display engine at the size of the mode, scaled to the raster the video BIOS set (the monitor's
// mode), with its aspect kept. Every mode up to that raster is offered at 1, 2 and 3 bytes per pixel: the driver
// converts what it is given to 32 bits, the 1 byte indexes through a palette of its own (the VGA DAC does not reach
// the display engine's output), and asks for a redraw when that palette changes. The text console is a 640x480
// frame buffer, as the one GRUB left.
class NvidiaPascalSvga : public SvgaDriver {
public:
    // Takes the display (NvidiaPascal::start()) and returns the driver, or null; the caller then sets a mode
    static SvgaDriver *create();

    const char *name() const override { return "NVIDIA Pascal display engine"; }
    uint32_t videoMemoryBytes() const override { return SURFACE_BYTES; }
    bool canSet(unsigned width, unsigned height, unsigned bytesPerPixel) const override;
    bool setMode(unsigned width, unsigned height, unsigned bytesPerPixel) override;
    void leaveMode() override {}
    volatile uint8_t *frameBuffer() const override { return surface; }
    uint32_t bytesPerLine() const override { return pitch; }
    void writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) override;
    void writeSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count) override;
    void fillRect(unsigned x, unsigned y, unsigned width, unsigned height, unsigned bytesPerPixel,
                  uint32_t color) override;
    bool loadPalette(const uint8_t *rgb) override;
    bool supportsFramebufferText() const override { return true; }
    bool vgaRegisterModes() const override { return false; }
    unsigned framebufferWidth() const override { return CONSOLE_WIDTH; }
    unsigned framebufferHeight() const override { return CONSOLE_HEIGHT; }

private:
    static constexpr uint64_t SURFACE_VRAM = 0x02000000;
    static constexpr uint32_t SURFACE_BYTES = 16u << 20;
    static constexpr unsigned CONSOLE_WIDTH = 640;
    static constexpr unsigned CONSOLE_HEIGHT = 480;

    NvidiaPascalSvga(volatile uint8_t *frame);
    static uint32_t pitchFor(unsigned width) { return (width * 4 + 255) & ~255u; }
    uint32_t convert(unsigned bytesPerPixel, uint32_t color) const;

    volatile uint8_t *surface;
    uint32_t pitch;
    unsigned width;
    unsigned height;
    // The 256 colours of the 1 byte modes, as 0xAARRGGBB
    uint32_t palette[256];
};

#endif
