#ifndef __NVIDIA_PRE_TESLA_2D__
#define __NVIDIA_PRE_TESLA_2D__
#include <stdint.h>
#include "drivers/video/nvidiaPreTesla/NvidiaInfo.h"
#include "drivers/video/svga/SvgaPrimitives.h"

class NvidiaPreTesla;

// The 2D engine (PGRAPH, fed by PFIFO) of the NVIDIA chips NvidiaPreTesla drives, from the RIVA TNT to the GeForce 7,
// as the Linux nvidiafb driver (from the XFree86 nv driver, MIT style licence, (c) 2003 NVIDIA Corporation) runs it:
// one DMA channel whose 32 KB push buffer sits in video memory just above what the modes may use, eight subchannels
// bound to the NV04-style 2D objects (surfaces, ROP, pattern, clip, line, image blit, GDI rectangle, stretched blit),
// solid rectangle fills and screen to screen copies. Only the engine set-up differs between NV04 and NV40.
//
// Its primitives() replace Svga's frame buffer ones while acceleration is selected. Whatever the CPU then writes
// waits for the engine first, and an engine that stops answering is left alone: its primitives draw with the CPU
// until start() again.
class NvidiaPreTesla2d {
public:
    explicit NvidiaPreTesla2d(NvidiaPreTesla &owner);

    // Resets PFIFO and PGRAPH and loads the objects, the channel and a push buffer (nvidiafb's NVLoadStateExt and
    // NVResetGraphics). False when the engine does not run the first commands.
    bool start();
    // Points the engine at the frame buffer of the current mode.
    void setSurface();
    // Fills the screen of the current mode with black, without waiting for it. Nothing unless usable().
    void clear();
    // Returns once the engine has run everything it was sent; false when it stops answering.
    bool idle();
    const SvgaPrimitives *primitives() const { return &PRIMITIVES; }
    void describe(NvidiaInfo &out) const;
    // Draws with the engine in video memory the screen does not show and compares the result with what the CPU
    // expects, then times fills and copies by both. Leaves the engine pointed at the current mode.
    bool selfTest(NvidiaInfo::Test &out);

private:
    static constexpr uint32_t PUSH_BUFFER_BYTES = 32 * 1024;
    static constexpr uint32_t DMA_MAX = PUSH_BUFFER_BYTES / 4 - 1;
    // A hardware race: the engine must not jump to the put offset, so put stays this many words past a jump.
    static constexpr uint32_t SKIPS = 8;
    static constexpr uint64_t TIMEOUT_NS = 250000000;

    // Methods, with the subchannel (0 to 7) in bits 13 to 15.
    static constexpr uint32_t SURFACE_FORMAT = 0x0300;
    static constexpr uint32_t ROP_SET = 0x2300;
    static constexpr uint32_t PATTERN_FORMAT = 0x4300;
    static constexpr uint32_t PATTERN_COLOR_0 = 0x4310;
    static constexpr uint32_t CLIP_POINT = 0x6300;
    static constexpr uint32_t LINE_FORMAT = 0x8300;
    static constexpr uint32_t BLIT_POINT_SOURCE = 0xA300;
    static constexpr uint32_t RECT_FORMAT = 0xC300;
    static constexpr uint32_t RECT_SOLID_COLOR = 0xC3FC;
    static constexpr uint32_t RECT_SOLID_RECTS = 0xC400;

    static constexpr uint32_t PMC = 0x000000;
    static constexpr uint32_t PFIFO = 0x002000;
    static constexpr uint32_t PTIMER = 0x009000;
    static constexpr uint32_t PFB = 0x100000;
    static constexpr uint32_t PGRAPH = 0x400000;
    static constexpr uint32_t PGRAPH_STATUS = 0x400700;
    static constexpr uint32_t PRAMIN = 0x710000;
    static constexpr uint32_t USER_FIFO = 0x800000;
    static constexpr uint32_t USER_DMA_PUT = USER_FIFO + 0x40;
    static constexpr uint32_t USER_DMA_GET = USER_FIFO + 0x44;

    static NvidiaPreTesla2d *running;
    static const SvgaPrimitives PRIMITIVES;

    static void enginePlot(unsigned x, unsigned y, unsigned bytesPerPixel, uint32_t color);
    static void engineSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count);
    static void engineFill(unsigned x, unsigned y, unsigned width, unsigned height, unsigned bytesPerPixel,
                           uint32_t color);
    static bool engineCopy(unsigned fromX, unsigned fromY, unsigned toX, unsigned toY, unsigned width,
                           unsigned height);
    static void engineSync();

    uint32_t read32(uint32_t reg) const;
    void write32(uint32_t reg, uint32_t value);
    void loadEngine();
    void loadNv04Graph();
    void loadNv40Graph();
    void loadFifo();
    // Whether the primitives may use the engine: started, answering, pointed at the mode on the screen.
    bool usable() const;
    void lockup(const char *what);

    void dmaNext(uint32_t value) { pushBuffer[current++] = value; }
    // False once the engine has stopped answering: then nothing may follow with dmaNext().
    bool dmaStart(uint32_t method, uint32_t size);
    void dmaWait(uint32_t size);
    void writePut(uint32_t value);
    uint32_t readGet() const;
    void kickoff();

    void surface(unsigned bytes, uint32_t surfacePitch, uint32_t offset, unsigned clipWidth, unsigned clipHeight);
    void setRopCopy();
    void fill(unsigned x, unsigned y, unsigned width, unsigned height, uint32_t color);
    void copy(unsigned fromX, unsigned fromY, unsigned toX, unsigned toY, unsigned width, unsigned height);

    NvidiaPreTesla &display;
    volatile uint32_t *pushBuffer;
    uint32_t pushBufferOffset;
    uint32_t put;
    uint32_t current;
    uint32_t freeWords;
    uint32_t currentRop;
    bool started;
    bool lockedUp;
    bool busy;
    bool surfaceReady;
    uint64_t fills;
    uint64_t copies;
    uint64_t kickoffs;
    uint32_t lockups;
};

#endif
