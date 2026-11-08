#ifndef __NV4X_PLL__
#define __NV4X_PLL__
#include <stddef.h>
#include <stdint.h>

// The pixel clock synthesizer (VPLL) of the GeForce 6 / 7: a crystal reference divided by M1, multiplied by N1 in a
// first VCO, optionally divided by M2 and multiplied by N2 in a second VCO, and divided by 2^log2P. Its limits come
// from the PLL limits table of the video BIOS, which the system BIOS leaves shadowed at 0xC0000. The search for the
// coefficients follows nouveau's (nvkm/subdev/clk/pllnv04.c, MIT licensed).
class Nv4xPll {
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

    // Reads the VPLL limits and the BIOS chip version (the second number of "05.73.22.51") from a video BIOS image.
    static bool fromVideoBios(const volatile uint8_t *rom, size_t bytes, Limits &limits, uint8_t &chipVersion);
    // The limits of the G73 (GeForce 7600 GS) BIOS 05.73.22.51, for a BIOS image that cannot be read.
    static const Limits &fallback();
    // The coefficients nearest to khz. Two stages are used only when twoStages is set and the limits have a second
    // VCO. False when no combination is within the limits.
    static bool calculate(const Limits &limits, uint32_t referenceKhz, uint8_t chipVersion, uint32_t khz,
                          bool twoStages, Coefficients &out);

private:
    static bool single(const Limits &limits, uint32_t crystal, uint8_t chipVersion, uint32_t khz, Coefficients &out);
    static bool dual(const Limits &limits, uint32_t crystal, uint8_t chipVersion, uint32_t khz, Coefficients &out);
};

#endif
