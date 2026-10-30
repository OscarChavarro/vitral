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
    virtual bool supportsFramebufferText() const { return false; }
    virtual unsigned framebufferWidth() const { return 0; }
    virtual unsigned framebufferHeight() const { return 0; }
};

#endif
