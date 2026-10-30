#ifndef __BOCHS_VBE__
#define __BOCHS_VBE__
#include "drivers/pci/Pci.h"
#include "drivers/video/svga/SvgaDriver.h"

class BochsVbe : public SvgaDriver {
public:
    static SvgaDriver *probe();

    const char *name() const override { return "Bochs VBE"; }
    uint32_t videoMemoryBytes() const override { return memoryBytes; }
    bool canSet(unsigned width, unsigned height, unsigned bytesPerPixel) const override;
    bool setMode(unsigned width, unsigned height, unsigned bytesPerPixel) override;
    void leaveMode() override;
    volatile uint8_t *frameBuffer() const override { return frame; }
    uint32_t bytesPerLine() const override { return pitch; }
    void writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) override;

private:
    static constexpr uint16_t INDEX_PORT = 0x1CE;
    static constexpr uint16_t DATA_PORT = 0x1CF;
    static constexpr uint16_t ID = 0;
    static constexpr uint16_t XRES = 1;
    static constexpr uint16_t YRES = 2;
    static constexpr uint16_t BPP = 3;
    static constexpr uint16_t ENABLE = 4;
    static constexpr uint16_t BANK = 5;
    static constexpr uint16_t VIRT_WIDTH = 6;
    static constexpr uint16_t VIRT_HEIGHT = 7;
    static constexpr uint16_t X_OFFSET = 8;
    static constexpr uint16_t Y_OFFSET = 9;
    static constexpr uint16_t VIDEO_MEMORY_64K = 10;
    static constexpr uint16_t ID_FIRST = 0xB0C0;
    static constexpr uint16_t ID_LAST = 0xB0CF;
    static constexpr uint16_t ID_MEMORY_SIZE = 0xB0C4;
    static constexpr uint16_t ENABLED = 0x01;
    static constexpr uint16_t LFB_ENABLED = 0x40;
    static constexpr unsigned MAX_WIDTH = 8192;
    static constexpr unsigned MAX_HEIGHT = 8192;

    BochsVbe(uint32_t memory, uint64_t lfbAddress)
        : memoryBytes(memory), frame((volatile uint8_t *)lfbAddress), pitch(0) {}

    static uint16_t read(uint16_t reg);
    static void write(uint16_t reg, uint16_t value);

    uint32_t memoryBytes;
    volatile uint8_t *frame;
    uint32_t pitch;
};

#endif
