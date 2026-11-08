#ifndef __NVIDIA_NV04__
#define __NVIDIA_NV04__
#include <stdint.h>
#include "drivers/video/nvidiaPreTesla/NvidiaPreTesla.h"

// The RIVA TNT (NV04) and TNT2 / Vanta / TNT2 M64 (NV05, the Fahrenheit family) on top of NvidiaPreTesla: one head,
// one DDC bus, a single stage pixel clock with fixed limits and the memory arbitration computed from the memory and
// core clocks, as the Linux nvidiafb driver (from the XFree86 nv driver, MIT style licence) programs them for
// NV_ARCH_04. Not yet tried on real hardware.
class NvidiaNv04 : public NvidiaPreTesla {
public:
    explicit NvidiaNv04(const Card &found) : NvidiaPreTesla(found) {}

protected:
    const char *familyName() const override { return "NV04"; }
    void initClocks() override;
    const uint8_t *extendedRegisters(unsigned &count) const override;
    const uint8_t (*ddcPorts(unsigned &count) const)[2] override;
    // The RAMDAC: 250 MHz on the TNT, 300 MHz on the TNT2 family.
    uint32_t maxPixelClockKhz() const override { return card.chipset == 0x04 ? 250000 : 300000; }
    // The pitch has 11 bits (CR13 and CR19) of 8 bytes.
    unsigned maxOffset() const override { return 0x7FF; }
    uint32_t engineReserveBytes() const override { return 128u << 10; }
    bool coefficientsFor(uint32_t khz, NvidiaPll::Coefficients &out) const override;
    void saveFamily(HeadState &s) override;
    void computeFamily(const DisplayTiming &t, unsigned bytes, uint32_t newPitch, const NvidiaPll::Coefficients &pll,
                       HeadState &s) override;
    void loadFamily(const HeadState &s) override;
    void logState(const char *what, const HeadState &s) override;
    void clocks(uint32_t &coreKhz, uint32_t &memoryKhz) const override;
    unsigned memoryBusBits() const override;
    void describeFamily(NvidiaInfo &out) const override;

private:
    static constexpr uint32_t PFB_CONFIG_1 = 0x100204;

    // The CRTC FIFO's burst size (CR1B) and low water mark (CR20) for a pixel clock and depth, or false when
    // nvidiafb's model finds none and the BIOS values stay.
    bool arbitration(uint32_t pixelKhz, unsigned bitsPerPixel, uint8_t &burst, uint8_t &lowWaterMark) const;
};

#endif
