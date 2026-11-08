#ifndef __NVIDIA_NV4X__
#define __NVIDIA_NV4X__
#include <stdint.h>
#include "drivers/pci/Pci.h"
#include "drivers/video/edid/DisplayTiming.h"
#include "drivers/video/edid/Edid.h"
#include "drivers/video/nvidiaNv4x/Nv4xPll.h"
#include "drivers/video/svga/SvgaDriver.h"

// Mode setting for the NVIDIA GeForce 6 and 7 (NV40 to G73, the "NV4x" or Curie family): linear frame buffer modes
// with 8, 16 (RGB 565) and 32 bits per pixel on the analog output that the video BIOS drives at boot, at the timings
// of the monitor's EDID or the VESA ones it accepts. The video BIOS has already initialized the card (memory, clocks,
// output routing); this only programs head A's CRTC, its pixel clock (VPLL) and the RAMDAC's pixel format, the way
// nouveau's dispnv04 code (MIT licensed) does, and puts the boot state back on leaveMode() so that the VGA text mode
// can be programmed again. No acceleration yet: the 2D engine (PGRAPH) is left alone.
class NvidiaNv4x : public SvgaDriver {
public:
    static SvgaDriver *probe();
    ~NvidiaNv4x() override;

    const char *name() const override { return "NVIDIA NV4x"; }
    uint32_t videoMemoryBytes() const override { return apertureBytes; }
    bool canSet(unsigned width, unsigned height, unsigned bytesPerPixel) const override;
    bool setMode(unsigned width, unsigned height, unsigned bytesPerPixel) override;
    void leaveMode() override;
    volatile uint8_t *frameBuffer() const override { return frame; }
    uint32_t bytesPerLine() const override { return pitch; }
    void writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) override;
    void writeSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count) override;

private:
    static constexpr uint32_t PMC_BOOT_0 = 0x000000;
    static constexpr uint32_t PFB_MEMORY_SIZE = 0x10020C;
    static constexpr uint32_t PEXTDEV_STRAPS = 0x101000;
    static constexpr uint32_t PLL_CLOCK_GATE = 0x00C040;
    static constexpr uint32_t PVIO = 0x0C0000;
    static constexpr uint32_t PCRTC_START = 0x600800;
    static constexpr uint32_t PCRTC_CONFIG = 0x600804;
    static constexpr uint32_t PCRTC_830 = 0x600830;
    static constexpr uint32_t PCRTC_834 = 0x600834;
    static constexpr uint32_t PCIO = 0x601000;
    static constexpr uint32_t PRAMDAC_CURSOR_SYNC = 0x680404;
    static constexpr uint32_t PRAMDAC_VPLL = 0x680508;
    static constexpr uint32_t PRAMDAC_PLL_SELECT = 0x68050C;
    static constexpr uint32_t PRAMDAC_DAC_CLOCK = 0x68052C;
    static constexpr uint32_t PRAMDAC_VPLL_B = 0x680578;
    static constexpr uint32_t PRAMDAC_580 = 0x680580;
    static constexpr uint32_t PRAMDAC_GENERAL_CONTROL = 0x680600;
    static constexpr uint32_t PRAMDAC_630 = 0x680630;
    static constexpr uint32_t PRAMDAC_TV_SETUP = 0x680700;
    static constexpr uint32_t PRAMDAC_8C0 = 0x6808C0;
    static constexpr uint32_t PRAMDAC_900 = 0x680900;
    static constexpr uint32_t PRAMDAC_A20 = 0x680A20;
    static constexpr uint32_t PRAMDAC_A24 = 0x680A24;
    static constexpr uint32_t PRAMDAC_A34 = 0x680A34;
    static constexpr uint32_t PDIO = 0x681000;

    static constexpr uint8_t CR_UNLOCK = 0x1F;
    static constexpr uint8_t CR_SHADOW_LOCK = 0x21;
    static constexpr uint8_t CR_PIXEL = 0x28;
    static constexpr uint8_t CR_OWNER = 0x44;
    static constexpr unsigned STANDARD_CRTC = 25;
    static constexpr unsigned MAX_PIXEL_CLOCK_KHZ = 400000;
    static constexpr uint32_t PITCH_ALIGN = 64;

    // What a mode change writes, for head A: the VGA registers, the extended CRTC registers, PCRTC and PRAMDAC.
    class HeadState {
    public:
        uint8_t misc;
        uint8_t sequencer[5];
        uint8_t crtc[STANDARD_CRTC];
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

    NvidiaNv4x(volatile uint8_t *registers, volatile uint8_t *vram, uint32_t aperture, unsigned chip)
        : mmio(registers), frame(vram), apertureBytes(aperture), chipset(chip), chipVersion(0), crystalKhz(0),
          pitch(0), modeBytes(0), scanBytes(0), width(0), height(0), boot(nullptr), mode(nullptr) {}

    bool init();
    bool timingFor(unsigned newWidth, unsigned newHeight, DisplayTiming &out) const;
    bool coefficientsFor(uint32_t khz, Nv4xPll::Coefficients &out) const;
    void save(HeadState &s);
    void logState(const char *what, const HeadState &s);
    void compute(const DisplayTiming &t, unsigned bytes, uint32_t newPitch, const Nv4xPll::Coefficients &pll,
                 HeadState &s);
    void load(const HeadState &s);
    void writePll(uint32_t pll, uint32_t pllB, uint32_t value580);
    void protect(bool on);
    bool readEdid(uint8_t drive, uint8_t sense, uint8_t *block);

    uint32_t read32(uint32_t reg) const { return *(volatile uint32_t *)(mmio + reg); }
    void write32(uint32_t reg, uint32_t value) { *(volatile uint32_t *)(mmio + reg) = value; }
    uint8_t read8(uint32_t reg) const { return mmio[reg]; }
    void write8(uint32_t reg, uint8_t value) { mmio[reg] = value; }
    uint8_t crtcRead(uint8_t index);
    void crtcWrite(uint8_t index, uint8_t value);
    uint8_t seqRead(uint8_t index);
    void seqWrite(uint8_t index, uint8_t value);
    uint8_t grRead(uint8_t index);
    void grWrite(uint8_t index, uint8_t value);
    uint8_t attrRead(uint8_t index);
    void attrWrite(uint8_t index, uint8_t value);

    // DDC: a bit-banged I2C bus on a pair of extended CRTC registers.
    void ddcDrive(uint8_t drive, uint8_t bit, bool high);
    bool ddcSense(uint8_t sense, uint8_t bit);
    bool ddcClockHigh(uint8_t drive, uint8_t sense);
    void ddcStart(uint8_t drive, uint8_t sense);
    void ddcStop(uint8_t drive, uint8_t sense);
    bool ddcWrite(uint8_t drive, uint8_t sense, uint8_t byte);
    uint8_t ddcRead(uint8_t drive, uint8_t sense, bool acknowledge);

    volatile uint8_t *mmio;
    volatile uint8_t *frame;
    uint32_t apertureBytes;
    unsigned chipset;
    uint8_t chipVersion;
    uint32_t crystalKhz;
    Nv4xPll::Limits pllLimits;
    Edid edid;
    uint32_t pitch;
    unsigned modeBytes;
    unsigned scanBytes;
    unsigned width;
    unsigned height;
    HeadState *boot;
    HeadState *mode;
};

#endif
