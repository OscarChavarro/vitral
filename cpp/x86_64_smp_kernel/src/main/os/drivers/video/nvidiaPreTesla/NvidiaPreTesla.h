#ifndef __NVIDIA_PRE_TESLA__
#define __NVIDIA_PRE_TESLA__
#include <stddef.h>
#include <stdint.h>
#include "drivers/pci/Pci.h"
#include "drivers/video/edid/DisplayTiming.h"
#include "drivers/video/edid/Edid.h"
#include "drivers/video/nvidiaPreTesla/NvidiaInfo.h"
#include "drivers/video/nvidiaPreTesla/NvidiaPll.h"
#include "drivers/video/svga/SvgaDriver.h"

class NvidiaPreTesla2d;

// What the NVIDIA chips before Tesla (G80) that this kernel drives have in common, from the RIVA TNT (NV04) and TNT2
// (NV05) to the GeForce 6 and 7 (NV40 to G73): their identification, the register and frame buffer apertures, the VGA
// registers behind PCIO / PVIO / PDIO, the DDC bus and the monitor's EDID, the timings of a mode and the CRTC values
// all of them share, and the frame buffer writes. The video BIOS has initialized the card (memory, clocks, output
// routing); a mode change programs head A's CRTC and RAMDAC for the analog output and leaveMode() puts the boot state
// back. A family adds its pixel clock, its other extended registers and the RAMDAC: NvidiaNv04 (TNT / TNT2) and
// NvidiaNv4x (GeForce 6 / 7). The 2D engine is a separate driver, NvidiaPreTesla2d, started only when acceleration is
// selected (Svga::setAccelerated, the `nvidia` program).
class NvidiaPreTesla : public SvgaDriver {
public:
    enum class Family : uint8_t { Nv04, Nv4x };

    static SvgaDriver *probe();
    // The card probe() found, or null.
    static NvidiaPreTesla *instance() { return probed; }
    // Syscalls::nvidia(): an operation of NvidiaInfo on the card. 0, or a negative error.
    static int64_t control(uint32_t operation, NvidiaInfo *out);

    ~NvidiaPreTesla() override;

    const char *name() const override { return chipName; }
    uint32_t videoMemoryBytes() const override { return card.apertureBytes; }
    bool canSet(unsigned width, unsigned height, unsigned bytesPerPixel) const override;
    bool setMode(unsigned width, unsigned height, unsigned bytesPerPixel) override;
    void leaveMode() override;
    volatile uint8_t *frameBuffer() const override { return card.frame; }
    uint32_t bytesPerLine() const override { return pitch; }
    void writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) override;
    void writeSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count) override;
    void fillRect(unsigned x, unsigned y, unsigned width, unsigned height, unsigned bytesPerPixel,
                  uint32_t color) override;
    const SvgaPrimitives *startAcceleration() override;
    void stopAcceleration() override;

protected:
    // What probe() learns of the card before choosing its family.
    class Card {
    public:
        Pci::Location at;
        uint16_t device;
        uint16_t implementation;
        uint16_t subsystemVendor;
        uint16_t subsystemDevice;
        uint8_t revision;
        unsigned chipset;
        uint32_t boot0;
        volatile uint8_t *mmio;
        uint64_t registersAddress;
        uint64_t registersBytes;
        volatile uint8_t *frame;
        uint64_t frameAddress;
        uint64_t frameBytes;
        uint32_t apertureBytes;
        uint32_t memoryBytes;
    };

    // What a mode change writes, for head A: the VGA registers, the extended CRTC registers and the 32-bit PCRTC,
    // PRAMDAC and PLL registers of the family (a family leaves the ones it lacks alone).
    class HeadState {
    public:
        uint8_t misc;
        uint8_t sequencer[5];
        uint8_t crtc[25];
        uint8_t graphics[9];
        uint8_t attribute[21];
        uint8_t extended[256];
        uint8_t palette[768];
        uint32_t vpll;
        uint32_t vpllB;
        uint32_t ramdac580;
        uint32_t pllSelect;
        uint32_t generalControl;
        uint32_t cursorSync;
        uint32_t ramdac630;
        uint32_t tvSetup;
        uint32_t ramdac8c0;
        uint32_t ramdac900;
        uint32_t ramdacA20;
        uint32_t ramdacA24;
        uint32_t ramdacA34;
        uint32_t crtcConfig;
        uint32_t crtcStart;
        uint32_t crtc830;
        uint32_t crtc834;
    };

    static constexpr uint32_t PMC_BOOT_0 = 0x000000;
    static constexpr uint32_t PEXTDEV_BOOT_0 = 0x101000;
    static constexpr uint32_t PVIO = 0x0C0000;
    static constexpr uint32_t PCRTC_START = 0x600800;
    static constexpr uint32_t PCIO = 0x601000;
    static constexpr uint32_t PRAMDAC_NVPLL = 0x680500;
    static constexpr uint32_t PRAMDAC_MPLL = 0x680504;
    static constexpr uint32_t PRAMDAC_VPLL = 0x680508;
    static constexpr uint32_t PRAMDAC_PLL_SELECT = 0x68050C;
    static constexpr uint32_t PRAMDAC_GENERAL_CONTROL = 0x680600;
    static constexpr uint32_t PDIO = 0x681000;
    static constexpr uint8_t CR_UNLOCK = 0x1F;
    static constexpr uint8_t CR_PIXEL = 0x28;
    static constexpr unsigned STANDARD_CRTC = 25;
    static constexpr uint32_t PITCH_ALIGN = 64;
    // nvidiafb: the 2D engine addresses at most 64 MB, and its push buffer and instance memory take the top of it.
    static constexpr uint32_t ENGINE_MAP_LIMIT = 64u << 20;

    explicit NvidiaPreTesla(const Card &found);

    // The family's part of the work. initHead() runs first, with the extended registers unlocked: false leaves the
    // card alone. initClocks() reads the PLL limits and the crystal.
    virtual const char *familyName() const = 0;
    virtual bool initHead() { return true; }
    virtual void initClocks() = 0;
    virtual const uint8_t *extendedRegisters(unsigned &count) const = 0;
    virtual const uint8_t (*ddcPorts(unsigned &count) const)[2] = 0;
    virtual uint32_t maxPixelClockKhz() const = 0;
    // The largest CRTC offset (pitch / 8) the extended registers can hold.
    virtual unsigned maxOffset() const = 0;
    // What the 2D engine reserves at the top of what it addresses.
    virtual uint32_t engineReserveBytes() const = 0;
    virtual bool coefficientsFor(uint32_t khz, NvidiaPll::Coefficients &out) const = 0;
    virtual void saveFamily(HeadState &s) = 0;
    virtual void computeFamily(const DisplayTiming &t, unsigned bytes, uint32_t newPitch,
                               const NvidiaPll::Coefficients &pll, HeadState &s) = 0;
    // Before the palette and the VGA registers are loaded, with the screen off; finishLoad() after, screen on.
    virtual void loadFamily(const HeadState &s) = 0;
    virtual void finishLoad(const HeadState &) {}
    virtual void logState(const char *what, const HeadState &s) = 0;
    virtual void clocks(uint32_t &coreKhz, uint32_t &memoryKhz) const = 0;
    virtual unsigned memoryBusBits() const = 0;
    virtual void describeFamily(NvidiaInfo &out) const = 0;

    uint32_t read32(uint32_t reg) const { return *(volatile uint32_t *)(card.mmio + reg); }
    void write32(uint32_t reg, uint32_t value) { *(volatile uint32_t *)(card.mmio + reg) = value; }
    uint8_t read8(uint32_t reg) const { return card.mmio[reg]; }
    void write8(uint32_t reg, uint8_t value) { card.mmio[reg] = value; }
    uint8_t crtcRead(uint8_t index) const;
    void crtcWrite(uint8_t index, uint8_t value);
    uint8_t seqRead(uint8_t index) const;
    void seqWrite(uint8_t index, uint8_t value);
    uint8_t grRead(uint8_t index) const;
    void grWrite(uint8_t index, uint8_t value);
    uint8_t attrRead(uint8_t index) const;
    void attrWrite(uint8_t index, uint8_t value);
    static void waitMicroseconds(unsigned microseconds);

    Card card;
    char chipName[NvidiaInfo::NAME_BYTES];
    uint32_t crystalKhz;
    Edid edid;
    uint8_t edidPort;
    uint32_t pitch;
    unsigned modeBytes;
    unsigned scanBytes;
    unsigned width;
    unsigned height;
    NvidiaPll::Coefficients modePll;
    HeadState *boot;
    HeadState *mode;

private:
    friend class NvidiaPreTesla2d;

    static NvidiaPreTesla *probed;

    bool init();
    void nameChip();
    uint32_t mapBytes() const;
    uint32_t usableBytes() const;
    bool timingFor(unsigned newWidth, unsigned newHeight, DisplayTiming &out) const;
    void save(HeadState &s);
    void computeStandard(const DisplayTiming &t, unsigned bytes, uint32_t newPitch, HeadState &s);
    void load(const HeadState &s);
    void protect(bool on);
    void describe(NvidiaInfo &out) const;
    bool test(NvidiaInfo::Test &out);

    // DDC: a bit-banged I2C bus on a pair of extended CRTC registers.
    bool readEdid(uint8_t drive, uint8_t sense, uint8_t *block);
    void ddcDrive(uint8_t drive, uint8_t bit, bool high);
    bool ddcSense(uint8_t sense, uint8_t bit) const;
    bool ddcClockHigh(uint8_t drive, uint8_t sense);
    void ddcStart(uint8_t drive, uint8_t sense);
    void ddcStop(uint8_t drive, uint8_t sense);
    bool ddcWrite(uint8_t drive, uint8_t sense, uint8_t byte);
    uint8_t ddcRead(uint8_t drive, uint8_t sense, bool acknowledge);

    NvidiaPreTesla2d *engine;
    bool accelerating;
};

#endif
