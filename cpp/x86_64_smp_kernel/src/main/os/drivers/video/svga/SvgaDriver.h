#ifndef __SVGA_DRIVER__
#define __SVGA_DRIVER__
#include <stdint.h>

class SvgaDriver {
public:
    virtual ~SvgaDriver() {}
    virtual const char *name() const = 0;
    virtual uint32_t videoMemoryBytes() const = 0;
    virtual bool canSet(unsigned width, unsigned height, unsigned bytesPerPixel) const = 0;
    virtual bool setMode(unsigned width, unsigned height, unsigned bytesPerPixel) = 0;
    virtual void leaveMode() = 0;
    virtual volatile uint8_t *frameBuffer() const = 0;
    virtual uint32_t bytesPerLine() const = 0;
    virtual void writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) = 0;
    // A run of count pixels of a row, least significant byte first. Drivers whose frame buffer has the same format can
    // copy it as a block.
    virtual void writeSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count) {
        for (unsigned i = 0; i < count; i++) {
            uint32_t color = 0;
            for (unsigned b = 0; b < bytesPerPixel; b++) {
                color |= (uint32_t)pixels[i * bytesPerPixel + b] << (8 * b);
            }
            writePixel(x + i, y, bytesPerPixel, color);
        }
    }
    virtual bool supportsFramebufferText() const { return false; }
    virtual unsigned framebufferWidth() const { return 0; }
    virtual unsigned framebufferHeight() const { return 0; }
};

#endif
