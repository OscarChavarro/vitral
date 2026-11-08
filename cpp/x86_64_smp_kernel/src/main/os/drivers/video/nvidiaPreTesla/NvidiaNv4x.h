#ifndef __NVIDIA_NV4X__
#define __NVIDIA_NV4X__
#include <stdint.h>
#include "drivers/video/nvidiaPreTesla/NvidiaPreTesla.h"

// The GeForce 6 and 7 (NV40 to G73, the "NV4x" or Curie family) on top of NvidiaPreTesla: linear modes with 8, 16
// (RGB 565) and 32 bits per pixel on the analog output the video BIOS drives at boot. It programs head A's extended
// CRTC registers, its two stage pixel clock (VPLL, limits from the video BIOS) and the RAMDAC's pixel format the way
// nouveau's dispnv04 code (MIT licensed) does. Verified on a GeForce 7600 GS AGP (G73).
class NvidiaNv4x : public NvidiaPreTesla {
public:
    explicit NvidiaNv4x(const Card &found)
        : NvidiaPreTesla(found), chipVersion(0), biosVersion(0), limitsFromBios(false), pllLimits() {}

protected:
    const char *familyName() const override { return "NV4x"; }
    bool initHead() override;
    void initClocks() override;
    const uint8_t *extendedRegisters(unsigned &count) const override;
    const uint8_t (*ddcPorts(unsigned &count) const)[2] override;
    uint32_t maxPixelClockKhz() const override { return MAX_PIXEL_CLOCK_KHZ; }
    unsigned maxOffset() const override { return 0xFFF; }
    uint32_t engineReserveBytes() const override { return 560u << 10; }
    bool coefficientsFor(uint32_t khz, NvidiaPll::Coefficients &out) const override;
    void saveFamily(HeadState &s) override;
    void computeFamily(const DisplayTiming &t, unsigned bytes, uint32_t newPitch, const NvidiaPll::Coefficients &pll,
                       HeadState &s) override;
    void loadFamily(const HeadState &s) override;
    void finishLoad(const HeadState &s) override;
    void logState(const char *what, const HeadState &s) override;
    void clocks(uint32_t &coreKhz, uint32_t &memoryKhz) const override;
    unsigned memoryBusBits() const override { return 0; }
    void describeFamily(NvidiaInfo &out) const override;

private:
    static constexpr uint32_t PMC_NVPLL = 0x004000;
    static constexpr uint32_t PMC_NVPLL_COEFFICIENTS = 0x004004;
    static constexpr uint32_t PMC_MPLL = 0x004020;
    static constexpr uint32_t PMC_MPLL_COEFFICIENTS = 0x004024;
    static constexpr uint32_t PLL_CLOCK_GATE = 0x00C040;
    static constexpr uint32_t PCRTC_CONFIG = 0x600804;
    static constexpr uint32_t PCRTC_830 = 0x600830;
    static constexpr uint32_t PCRTC_834 = 0x600834;
    static constexpr uint32_t PRAMDAC_CURSOR_SYNC = 0x680404;
    static constexpr uint32_t PRAMDAC_DAC_CLOCK = 0x68052C;
    static constexpr uint32_t PRAMDAC_VPLL_B = 0x680578;
    static constexpr uint32_t PRAMDAC_580 = 0x680580;
    static constexpr uint32_t PRAMDAC_630 = 0x680630;
    static constexpr uint32_t PRAMDAC_TV_SETUP = 0x680700;
    static constexpr uint32_t PRAMDAC_8C0 = 0x6808C0;
    static constexpr uint32_t PRAMDAC_900 = 0x680900;
    static constexpr uint32_t PRAMDAC_A20 = 0x680A20;
    static constexpr uint32_t PRAMDAC_A24 = 0x680A24;
    static constexpr uint32_t PRAMDAC_A34 = 0x680A34;
    static constexpr uint8_t CR_SHADOW_LOCK = 0x21;
    static constexpr uint8_t CR_OWNER = 0x44;
    static constexpr uint32_t MAX_PIXEL_CLOCK_KHZ = 400000;

    void writePll(uint32_t pll, uint32_t pllB, uint32_t value580);

    uint8_t chipVersion;
    uint32_t biosVersion;
    bool limitsFromBios;
    NvidiaPll::Limits pllLimits;
};

#endif
