#ifndef __SVGA_DRIVER__
#define __SVGA_DRIVER__
#include <stdint.h>
#include "drivers/video/svga/SvgaPrimitives.h"

class SvgaDriver {
public:
    virtual ~SvgaDriver() {}
    virtual const char *name() const = 0;
    virtual uint32_t videoMemoryBytes() const = 0;
    virtual bool canSet(unsigned width, unsigned height, unsigned bytesPerPixel) const = 0;
    // What the frame buffer then shows is up to the driver (it may keep old contents): the caller paints the screen.
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
    // A rectangle of one colour, written by the CPU.
    virtual void fillRect(unsigned x, unsigned y, unsigned width, unsigned height, unsigned bytesPerPixel,
                          uint32_t color) {
        for (unsigned row = 0; row < height; row++) {
            for (unsigned column = 0; column < width; column++) {
                writePixel(x + column, y + row, bytesPerPixel, color);
            }
        }
    }
    // The drawing operations of the driver's 2D engine, started, or null when it has none or the engine does not
    // start. Svga calls stopAcceleration() once it has put its frame buffer operations back.
    virtual const SvgaPrimitives *startAcceleration() { return nullptr; }
    virtual void stopAcceleration() {}
    // The palette of the 1 byte modes, 256 entries of the VGA DAC's 6 bit red, green and blue. True when the driver
    // keeps it (the indexes are converted as they are written, so what the screen shows must be written again); false
    // when the VGA DAC is the card's palette.
    virtual bool loadPalette(const uint8_t *) { return false; }
    virtual bool supportsFramebufferText() const { return false; }
    // False when the card was left by the firmware in a mode of its own (a boot framebuffer on an unknown card): the
    // standard VGA register sets of the text and planar modes then do not take the display over, they garble it.
    virtual bool vgaRegisterModes() const { return true; }
    virtual unsigned framebufferWidth() const { return 0; }
    virtual unsigned framebufferHeight() const { return 0; }
};

#endif
