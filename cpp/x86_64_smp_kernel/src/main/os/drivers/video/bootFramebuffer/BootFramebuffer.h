#ifndef __BOOT_FRAMEBUFFER__
#define __BOOT_FRAMEBUFFER__
#include "drivers/video/svga/SvgaDriver.h"

class BootFramebuffer : public SvgaDriver {
public:
    static SvgaDriver *probe();

    const char *name() const override { return "boot framebuffer"; }
    uint32_t videoMemoryBytes() const override { return bytes; }
    bool canSet(unsigned width, unsigned height, unsigned bytesPerPixel) const override;
    bool setMode(unsigned width, unsigned height, unsigned bytesPerPixel) override;
    void leaveMode() override {}
    volatile uint8_t *frameBuffer() const override { return frame; }
    uint32_t bytesPerLine() const override { return pitch; }
    void writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) override;
    bool supportsFramebufferText() const override { return true; }
    unsigned framebufferWidth() const override { return width; }
    unsigned framebufferHeight() const override { return height; }

private:
    BootFramebuffer(uint64_t address, uint32_t newPitch, uint32_t newWidth, uint32_t newHeight, uint8_t newBytes,
                    uint8_t newRedPosition, uint8_t newRedSize, uint8_t newGreenPosition, uint8_t newGreenSize,
                    uint8_t newBluePosition, uint8_t newBlueSize)
        : frame((volatile uint8_t *)address), pitch(newPitch), width(newWidth), height(newHeight),
          bytes((uint32_t)((uint64_t)newPitch * newHeight)), pixelBytes(newBytes), redPosition(newRedPosition),
          redSize(newRedSize), greenPosition(newGreenPosition), greenSize(newGreenSize), bluePosition(newBluePosition),
          blueSize(newBlueSize) {}

    volatile uint8_t *frame;
    uint32_t pitch;
    uint32_t width;
    uint32_t height;
    uint32_t bytes;
    uint8_t pixelBytes;
    uint8_t redPosition;
    uint8_t redSize;
    uint8_t greenPosition;
    uint8_t greenSize;
    uint8_t bluePosition;
    uint8_t blueSize;
};

#endif
