#ifndef __NVIDIA_PLL__
#define __NVIDIA_PLL__
#include <stddef.h>
#include <stdint.h>

// The pixel clock synthesizer (VPLL) of the NVIDIA chips before Tesla: a crystal reference divided by M1, multiplied
// by N1 in a first VCO, on the GeForce 6 / 7 optionally divided by M2 and multiplied by N2 in a second VCO, and divided
// by 2^log2P. The GeForce 6 / 7 limits come from the PLL limits table of the video BIOS, which the system BIOS leaves
// shadowed at 0xC0000, and the search follows nouveau's (nvkm/subdev/clk/pllnv04.c, MIT licensed). The RIVA TNT and
// TNT2 BIOS has no such table: their single stage PLL uses the fixed limits of the XFree86 / Linux nv driver.
class NvidiaPll {
public:
    class Limits {
    public:
        uint32_t vco1MinKhz;
        uint32_t vco1MaxKhz;
        uint32_t vco2MinKhz;
        uint32_t vco2MaxKhz;
        uint32_t input1MinKhz;
        uint32_t input1MaxKhz;
        uint32_t input2MinKhz;
        uint32_t input2MaxKhz;
        uint8_t n1Min;
        uint8_t n1Max;
        uint8_t m1Min;
        uint8_t m1Max;
        uint8_t n2Min;
        uint8_t n2Max;
        uint8_t m2Min;
        uint8_t m2Max;
        uint8_t log2PMax;
        // 0 when the table leaves it to the crystal strap.
        uint32_t referenceKhz;
    };

    class Coefficients {
    public:
        uint8_t n1;
        uint8_t m1;
        uint8_t n2;
        uint8_t m2;
        uint8_t log2P;
        bool twoStages;
        uint32_t khz;
    };

    // Reads the VPLL limits, the BIOS version (05.73.22.51 is 0x05732251) and its chip version (the second number)
    // from a video BIOS image with a BIT table (GeForce FX and later).
    static bool fromVideoBios(const volatile uint8_t *rom, size_t bytes, Limits &limits, uint8_t &chipVersion,
                              uint32_t &biosVersion);
    // The limits of the G73 (GeForce 7600 GS) BIOS 05.73.22.51, for a BIOS image that cannot be read.
    static const Limits &fallback();
    // The coefficients nearest to khz. Two stages are used only when twoStages is set and the limits have a second
    // VCO. False when no combination is within the limits.
    static bool calculate(const Limits &limits, uint32_t referenceKhz, uint8_t chipVersion, uint32_t khz,
                          bool twoStages, Coefficients &out);
    // The RIVA TNT / TNT2 VPLL (nvidiafb's CalcVClock): VCO 128-350 MHz, M 7-13 with a 13.5 MHz crystal and 8-14 with
    // a 14.318 MHz one, N up to 255, P up to 16. False when no combination is in range.
    static bool calculateNv04(uint32_t crystalKhz, uint32_t khz, Coefficients &out);

private:
    static bool single(const Limits &limits, uint32_t crystal, uint8_t chipVersion, uint32_t khz, Coefficients &out);
    static bool dual(const Limits &limits, uint32_t crystal, uint8_t chipVersion, uint32_t khz, Coefficients &out);
};

#endif
