#include "drivers/video/nvidiaPreTesla/NvidiaNv04.h"
#include "kernel/KernelLog.h"
#include "lib/vitralKernel/Format.h"

// The extended CRTC registers nvidiafb loads on an NV04 (NVLoadStateExt), in its order: the pitch and overflow bits,
// the pixel depth, the FIFO control, its burst size and low water mark, the interlace register.
static const uint8_t NV04_EXTENDED[] = {0x19, 0x1A, 0x25, 0x28, 0x2D, 0x1C, 0x1B, 0x20, 0x39};

// The one DDC bus of the TNT / TNT2 (drive register, sense register).
static const uint8_t NV04_DDC_PORTS[][2] = {{0x3F, 0x3E}};

// nvidiafb's input to the NV04 memory arbitration model (nv4_sim_state): clocks in kHz.
class Nv04ArbitrationInput {
public:
    int pixelKhz;
    int memoryKhz;
    int coreKhz;
    int pageMiss;
    int latency;
    int bitsPerPixel;
};

// nvidiafb's nv4CalcArbitration with video overlay and MP off (the memory width only matters with the overlay): the
// CRTC FIFO's low water mark (in bytes) for a burst of 128 bytes, false when the memory cannot feed the pixel clock.
static bool
nv04CalculateArbitration(const Nv04ArbitrationInput &in, int &lowWaterMark) {
    int cbs = 128;
    int nvclks = 2 + 2 + 1 + 2 + 1 + 1 + 1;
    int mclks = 5 + 3 + 1 + in.latency + 1 + 1 + 1 + 1;
    int mclkExtra = 3;
    bool valid = false;
    bool found = false;
    while (!found) {
        valid = true;
        found = true;
        int mclkLoop = mclks + mclkExtra;
        int usM = mclkLoop * 1000 * 1000 / in.memoryKhz;
        int usN = nvclks * 1000 * 1000 / in.coreKhz;
        int usP = nvclks * 1000 * 1000 / in.pixelKhz;
        int crtcDrainRate = in.pixelKhz * in.bitsPerPixel / 8;
        int crtPageMiss = 2 + 1;
        int cpmUs = crtPageMiss * in.pageMiss * 1000 * 1000 / in.memoryKhz;
        int usCrt = cpmUs + usM + usN + usP;
        int clwm = (int)((int64_t)usCrt * crtcDrainRate / (1000 * 1000)) + 1;
        int m1 = clwm + cbs - 512;
        int p1 = (int)((int64_t)m1 * in.pixelKhz / in.memoryKhz);
        p1 = p1 * in.bitsPerPixel / 8;
        if ((p1 < m1 && m1 > 0) || clwm > 519) {
            valid = false;
            found = false;
            if (mclkExtra == 0) {
                found = true;
            }
            mclkExtra--;
        }
        if (clwm < 384) {
            clwm = 384;
        }
        lowWaterMark = clwm;
    }
    return valid;
}

const uint8_t *
NvidiaNv04::extendedRegisters(unsigned &count) const {
    count = sizeof(NV04_EXTENDED);
    return NV04_EXTENDED;
}

const uint8_t (*NvidiaNv04::ddcPorts(unsigned &count) const)[2] {
    count = 1;
    return NV04_DDC_PORTS;
}

void
NvidiaNv04::initClocks() {
    crystalKhz = (read32(PEXTDEV_BOOT_0) & 0x40) ? 14318 : 13500;
    uint32_t coreKhz;
    uint32_t memoryKhz;
    clocks(coreKhz, memoryKhz);
    kprintf("Video: NV04: crystal %u kHz, core %u MHz, memory %u MHz on a %u-bit bus, pixel clock up to %u MHz\n",
            crystalKhz, coreKhz / 1000, memoryKhz / 1000, memoryBusBits(), maxPixelClockKhz() / 1000);
}

unsigned
NvidiaNv04::memoryBusBits() const {
    return (read32(PEXTDEV_BOOT_0) & 0x10) ? 128 : 64;
}

// nvidiafb's nvGetClocks for a single stage PLL: N * crystal / M / 2^P.
void
NvidiaNv04::clocks(uint32_t &coreKhz, uint32_t &memoryKhz) const {
    uint32_t pll = read32(PRAMDAC_MPLL);
    uint32_t m = pll & 0xFF;
    memoryKhz = m ? ((pll >> 8 & 0xFF) * crystalKhz / m) >> (pll >> 16 & 0x0F) : 0;
    pll = read32(PRAMDAC_NVPLL);
    m = pll & 0xFF;
    coreKhz = m ? ((pll >> 8 & 0xFF) * crystalKhz / m) >> (pll >> 16 & 0x0F) : 0;
}

// nvidiafb's nv4UpdateArbitrationSettings: the memory timings come from PFB_CONFIG_1.
bool
NvidiaNv04::arbitration(uint32_t pixelKhz, unsigned bitsPerPixel, uint8_t &burst, uint8_t &lowWaterMark) const {
    uint32_t coreKhz;
    uint32_t memoryKhz;
    clocks(coreKhz, memoryKhz);
    if (coreKhz == 0 || memoryKhz == 0 || pixelKhz == 0) {
        return false;
    }
    uint32_t config = read32(PFB_CONFIG_1);
    Nv04ArbitrationInput in;
    in.pixelKhz = (int)pixelKhz;
    in.memoryKhz = (int)memoryKhz;
    in.coreKhz = (int)coreKhz;
    in.latency = (int)(config & 0x0F);
    in.pageMiss = (int)(((config >> 4) & 0x0F) + ((config >> 31) & 0x01));
    in.bitsPerPixel = (int)bitsPerPixel;
    int bytes = 0;
    if (!nv04CalculateArbitration(in, bytes)) {
        return false;
    }
    // A 128 byte burst: 128 >> 4 = 8, whose base 2 logarithm is 3.
    burst = 3;
    lowWaterMark = (uint8_t)(bytes >> 3);
    return true;
}

bool
NvidiaNv04::coefficientsFor(uint32_t khz, NvidiaPll::Coefficients &out) const {
    if (!NvidiaPll::calculateNv04(crystalKhz, khz, out)) {
        return false;
    }
    uint32_t error = out.khz > khz ? out.khz - khz : khz - out.khz;
    return error <= khz / 200;
}

void
NvidiaNv04::saveFamily(HeadState &s) {
    s.vpll = read32(PRAMDAC_VPLL);
    s.pllSelect = read32(PRAMDAC_PLL_SELECT);
    s.generalControl = read32(PRAMDAC_GENERAL_CONTROL);
    s.crtcStart = read32(PCRTC_START);
}

void
NvidiaNv04::computeFamily(const DisplayTiming &, unsigned bytes, uint32_t, const NvidiaPll::Coefficients &pll,
                          HeadState &s) {
    uint8_t burst;
    uint8_t lowWaterMark;
    if (arbitration(pll.khz, bytes * 8, burst, lowWaterMark)) {
        s.extended[0x1B] = burst;
        s.extended[0x20] = lowWaterMark;
    } else {
        klogf("Video: NV04: no memory arbitration for a %u kHz pixel clock, keeping the BIOS one\n", pll.khz);
    }
    // VPLL, MPLL and NVPLL from their programmed coefficients, VCLK ratio /2 (nvidiafb, and nouveau below NV41).
    s.pllSelect = 0x10000700;
    s.vpll = (uint32_t)pll.log2P << 16 | (uint32_t)pll.n1 << 8 | pll.m1;
    // VGA state select and 8 bit palette; RGB 565 at 16 bits; DirectColor (PIXMIX bits) above 8 bits.
    s.generalControl = (bytes == 2 ? 0x00101100u : 0x00100100u) | (bytes != 1 ? 0x30u : 0);
    s.crtcStart = 0;
}

void
NvidiaNv04::loadFamily(const HeadState &s) {
    for (uint8_t index : NV04_EXTENDED) {
        crtcWrite(index, s.extended[index]);
    }
    write32(PRAMDAC_PLL_SELECT, s.pllSelect);
    write32(PRAMDAC_VPLL, s.vpll);
    write32(PRAMDAC_GENERAL_CONTROL, s.generalControl);
    write32(PCRTC_START, s.crtcStart);
}

void
NvidiaNv04::logState(const char *what, const HeadState &s) {
    klogf("Video: NV04 %s: VPLL %08x PLLSEL %08x GENCTRL %08x START %08x misc %02x SR1 %02x SR4 %02x CR11 %02x "
          "CR13 %02x CR17 %02x CR19 %02x CR1A %02x CR1B %02x CR1C %02x CR20 %02x CR25 %02x CR28 %02x CR2D %02x\n",
          what, s.vpll, s.pllSelect, s.generalControl, s.crtcStart, (uint32_t)s.misc, (uint32_t)s.sequencer[1],
          (uint32_t)s.sequencer[4], (uint32_t)s.crtc[0x11], (uint32_t)s.crtc[0x13], (uint32_t)s.crtc[0x17],
          (uint32_t)s.extended[0x19], (uint32_t)s.extended[0x1A], (uint32_t)s.extended[0x1B],
          (uint32_t)s.extended[0x1C], (uint32_t)s.extended[0x20], (uint32_t)s.extended[0x25],
          (uint32_t)s.extended[CR_PIXEL], (uint32_t)s.extended[0x2D]);
}

void
NvidiaNv04::describeFamily(NvidiaInfo &out) const {
    Format::print(out.familyName, sizeof(out.familyName), "NV04 (Fahrenheit: RIVA TNT / TNT2)");
    out.twoStagePll = false;
    out.vco1MinKhz = 128000;
    out.vco1MaxKhz = 350000;
}
