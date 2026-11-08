#include "drivers/video/nvidiaPreTesla/NvidiaNv4x.h"
#include "kernel/KernelLog.h"
#include "lib/vitralKernel/Format.h"

// The extended CRTC registers a mode change writes, in the order nouveau loads them.
static const uint8_t NV4X_EXTENDED[] = {
    0x19, 0x1A, 0x25, 0x28, 0x2D, 0x1C, 0x1B, 0x20, 0x47, 0x9F, 0x39, 0x3B, 0x41, 0x4B, 0x52, 0x42, 0x53, 0x54,
    0x59, 0x85, 0x86,
};

// The I2C buses of the DDC lines (drive register, sense register), as the DCB of the GeForce 6 / 7 BIOS lists them.
static const uint8_t NV4X_DDC_PORTS[][2] = {{0x3F, 0x3E}, {0x37, 0x36}, {0x51, 0x50}};

static const volatile uint8_t *const NV4X_VIDEO_BIOS = (const volatile uint8_t *)0xC0000;
static constexpr size_t NV4X_VIDEO_BIOS_BYTES = 128 * 1024;

// The BIOS version number of a chip, for a BIOS whose version cannot be read.
static uint8_t
nv4xBiosChip(unsigned chipset) {
    switch (chipset) {
    case 0x47:
        return 0x70;
    case 0x49:
        return 0x71;
    case 0x46:
        return 0x72;
    case 0x4B:
        return 0x73;
    default:
        return (uint8_t)chipset;
    }
}

const uint8_t *
NvidiaNv4x::extendedRegisters(unsigned &count) const {
    count = sizeof(NV4X_EXTENDED);
    return NV4X_EXTENDED;
}

const uint8_t (*NvidiaNv4x::ddcPorts(unsigned &count) const)[2] {
    count = sizeof(NV4X_DDC_PORTS) / sizeof(NV4X_DDC_PORTS[0]);
    return NV4X_DDC_PORTS;
}

bool
NvidiaNv4x::initHead() {
    uint8_t owner = crtcRead(CR_OWNER);
    if (owner == 3) {
        kprintf("Video: NV4x: the BIOS drives the display from head B, which is not supported\n");
        return false;
    }
    if (owner == 4) {
        // Heads tied: head B repeats head A. Untie them, as nouveau does, so that head A is programmed alone.
        crtcWrite(CR_OWNER, 0);
    }
    return true;
}

void
NvidiaNv4x::initClocks() {
    const volatile uint8_t *rom = NV4X_VIDEO_BIOS;
    if (rom[0] == 0x55 && rom[1] == 0xAA) {
        uint32_t pcir = (uint32_t)rom[0x18] | (uint32_t)rom[0x19] << 8;
        if (pcir + 8 < NV4X_VIDEO_BIOS_BYTES && rom[pcir] == 'P' && rom[pcir + 1] == 'C' && rom[pcir + 2] == 'I' &&
            rom[pcir + 3] == 'R' && rom[pcir + 4] == 0xDE && rom[pcir + 5] == 0x10) {
            limitsFromBios = NvidiaPll::fromVideoBios(rom, NV4X_VIDEO_BIOS_BYTES, pllLimits, chipVersion,
                                                      biosVersion);
        }
    }
    if (!limitsFromBios) {
        pllLimits = NvidiaPll::fallback();
        chipVersion = nv4xBiosChip(card.chipset);
        kprintf("Video: NV4x: no PLL limits in the video BIOS at 0xC0000, using the G73 ones\n");
    }
    if (chipVersion == 0) {
        chipVersion = nv4xBiosChip(card.chipset);
    }
    switch (read32(PEXTDEV_BOOT_0) & 0x00400040) {
    case 0x00000000:
        crystalKhz = 13500;
        break;
    case 0x00000040:
        crystalKhz = 14318;
        break;
    case 0x00400000:
        crystalKhz = 27000;
        break;
    default:
        crystalKhz = 25000;
        break;
    }
    kprintf("Video: NV4x: BIOS chip 0x%02x, crystal %u kHz, VPLL VCO %u-%u / %u-%u MHz, DAC clocks %08x %08x\n",
            (uint32_t)chipVersion, crystalKhz, pllLimits.vco1MinKhz / 1000, pllLimits.vco1MaxKhz / 1000,
            pllLimits.vco2MinKhz / 1000, pllLimits.vco2MaxKhz / 1000, read32(PRAMDAC_DAC_CLOCK),
            read32(PRAMDAC_DAC_CLOCK + 0x68));
}

// The registers a mode change depends on, for comparing the boot state with a programmed mode on real hardware.
void
NvidiaNv4x::logState(const char *what, const HeadState &s) {
    klogf("Video: NV4x %s: VPLL %08x %08x 580 %08x PLLSEL %08x GENCTRL %08x CRTCCFG %08x START %08x 900 %08x\n",
          what, s.vpll, s.vpllB, s.ramdac580, s.pllSelect, s.generalControl, s.crtcConfig, s.crtcStart, s.ramdac900);
    klogf("Video: NV4x %s: misc %02x SR1 %02x SR4 %02x CR11 %02x CR13 %02x CR17 %02x CR19 %02x CR1A %02x CR1B %02x "
          "CR1C %02x CR20 %02x CR21 %02x CR25 %02x CR28 %02x CR2D %02x CR41 %02x CR42 %02x CR44 %02x CR47 %02x "
          "CR4B %02x CR52 %02x C040 %08x\n",
          what, (uint32_t)s.misc, (uint32_t)s.sequencer[1], (uint32_t)s.sequencer[4], (uint32_t)s.crtc[0x11],
          (uint32_t)s.crtc[0x13], (uint32_t)s.crtc[0x17], (uint32_t)s.extended[0x19], (uint32_t)s.extended[0x1A],
          (uint32_t)s.extended[0x1B], (uint32_t)s.extended[0x1C], (uint32_t)s.extended[0x20],
          (uint32_t)s.extended[CR_SHADOW_LOCK], (uint32_t)s.extended[0x25], (uint32_t)s.extended[CR_PIXEL],
          (uint32_t)s.extended[0x2D], (uint32_t)s.extended[0x41], (uint32_t)s.extended[0x42],
          (uint32_t)crtcRead(CR_OWNER), (uint32_t)s.extended[0x47], (uint32_t)s.extended[0x4B],
          (uint32_t)s.extended[0x52], read32(PLL_CLOCK_GATE));
}

void
NvidiaNv4x::saveFamily(HeadState &s) {
    s.extended[CR_SHADOW_LOCK] = crtcRead(CR_SHADOW_LOCK);
    s.vpll = read32(PRAMDAC_VPLL);
    s.vpllB = read32(PRAMDAC_VPLL_B);
    s.ramdac580 = read32(PRAMDAC_580);
    s.pllSelect = read32(PRAMDAC_PLL_SELECT);
    s.generalControl = read32(PRAMDAC_GENERAL_CONTROL);
    s.cursorSync = read32(PRAMDAC_CURSOR_SYNC);
    s.ramdac630 = read32(PRAMDAC_630);
    s.tvSetup = read32(PRAMDAC_TV_SETUP);
    s.ramdac8c0 = read32(PRAMDAC_8C0);
    s.ramdac900 = read32(PRAMDAC_900);
    s.ramdacA20 = read32(PRAMDAC_A20);
    s.ramdacA24 = read32(PRAMDAC_A24);
    s.ramdacA34 = read32(PRAMDAC_A34);
    s.crtcConfig = read32(PCRTC_CONFIG);
    s.crtcStart = read32(PCRTC_START);
    s.crtc830 = read32(PCRTC_830);
    s.crtc834 = read32(PCRTC_834);
}

// The pixel clock synthesizer must not be changed while its clock runs into the display engine: nouveau gates it in
// 0xC040 (bits 16-17 for VPLL1) around the write.
void
NvidiaNv4x::writePll(uint32_t pll, uint32_t pllB, uint32_t value580) {
    if (read32(PRAMDAC_VPLL) == pll && read32(PRAMDAC_VPLL_B) == pllB && read32(PRAMDAC_580) == value580) {
        return;
    }
    uint32_t gate = read32(PLL_CLOCK_GATE);
    write32(PLL_CLOCK_GATE, gate & ~(3u << 16));
    if (read32(PRAMDAC_580) != value580) {
        write32(PRAMDAC_580, value580);
    }
    write32(PRAMDAC_VPLL_B, pllB);
    write32(PRAMDAC_VPLL, pll);
    write32(PLL_CLOCK_GATE, gate);
}

void
NvidiaNv4x::loadFamily(const HeadState &s) {
    write32(PRAMDAC_CURSOR_SYNC, s.cursorSync);
    writePll(s.vpll, s.vpllB, s.ramdac580);
    write32(PRAMDAC_PLL_SELECT, s.pllSelect);
    write32(PRAMDAC_GENERAL_CONTROL, s.generalControl);
    write32(PRAMDAC_630, s.ramdac630);
    write32(PRAMDAC_TV_SETUP, s.tvSetup);
    write32(PRAMDAC_8C0, s.ramdac8c0);
    write32(PRAMDAC_A20, s.ramdacA20);
    write32(PRAMDAC_A24, s.ramdacA24);
    write32(PRAMDAC_A34, s.ramdacA34);

    write32(PCRTC_830, s.crtc830);
    write32(PCRTC_834, s.crtc834);
    write32(PRAMDAC_900, s.ramdac900);
    write32(PCRTC_CONFIG, s.crtcConfig);
    // Connects the shadowed timing registers to the real ones, so that the writes below take effect.
    crtcWrite(CR_SHADOW_LOCK, (uint8_t)(boot->extended[CR_SHADOW_LOCK] | 0xFA));
    for (uint8_t index : NV4X_EXTENDED) {
        crtcWrite(index, s.extended[index]);
    }
    write32(PCRTC_START, s.crtcStart);
}

void
NvidiaNv4x::finishLoad(const HeadState &s) {
    crtcWrite(CR_SHADOW_LOCK, s.extended[CR_SHADOW_LOCK]);
}

bool
NvidiaNv4x::coefficientsFor(uint32_t khz, NvidiaPll::Coefficients &out) const {
    // Below half the first VCO's maximum, newer NV4x run the VPLL on its first stage only, as the blob does.
    bool twoStages = !(card.chipset > 0x40 && khz <= pllLimits.vco1MaxKhz / 2);
    if (!NvidiaPll::calculate(pllLimits, crystalKhz, chipVersion, khz, twoStages, out)) {
        return false;
    }
    uint32_t error = out.khz > khz ? out.khz - khz : khz - out.khz;
    return error <= khz / 200;
}

void
NvidiaNv4x::computeFamily(const DisplayTiming &t, unsigned bytes, uint32_t newPitch, const NvidiaPll::Coefficients &pll,
                          HeadState &s) {
    unsigned vDisplay = t.vDisplay - 1u;
    unsigned vStart = t.vSyncStart - 1u;
    unsigned vTotal = t.vTotal - 2u;
    unsigned vBlankStart = t.vDisplay - 1u;
    unsigned offset = newPitch / 8;

    uint8_t *ex = s.extended;
    ex[0x42] = (uint8_t)((offset >> 11 & 1) << 6);
    ex[0x41] = (uint8_t)((vBlankStart >> 11 & 1) << 6 | (vStart >> 11 & 1) << 4 | (vDisplay >> 11 & 1) << 2 |
                         (vTotal >> 11 & 1));
    // The arbitration nouveau uses from NV20 on: 512 byte bursts out of a 2 KB FIFO.
    ex[0x1B] = 4;
    ex[0x20] = 0xC0;
    ex[0x47] = 0x00;
    ex[0x3B] = 0x22;
    ex[0x4B] = (uint8_t)(boot->extended[0x4B] | 0x80);
    ex[0x52] = (uint8_t)(boot->extended[0x52] + 4);
    ex[0x53] = 0x00;
    ex[0x54] = 0x00;
    ex[0x59] = 0x00;
    ex[0x9F] = 0x01;
    ex[0x85] = 0xFF;
    ex[0x86] = 0x01;
    ex[CR_SHADOW_LOCK] = (uint8_t)(boot->extended[CR_SHADOW_LOCK] | 0xFA);

    s.crtcConfig = 0x2;
    s.crtcStart = 0;
    s.crtc830 = t.vDisplay - 3u;
    s.crtc834 = t.vDisplay - 1u;
    s.ramdac900 = boot->ramdac900 | 0x10000;
    s.cursorSync = 1u << 25;
    // PIXMIX on, VGA state select, 8 bit palette (6 bits in the indexed mode, as VGA programs expect), long pipe;
    // RGB 565 rather than 555 at 16 bits.
    s.generalControl = 0x20000130u | (bytes == 1 ? 0 : 0x00100000u) | (bytes == 2 ? 0x00001000u : 0);
    s.ramdac630 = 0;
    s.tvSetup = 0;
    s.ramdac8c0 = 0x100;
    s.ramdacA20 = 0;
    s.ramdacA24 = 0xFFFFF;
    s.ramdacA34 = 1;
    // VPLL1 from its programmed coefficients, VPLL2 kept as the BIOS selected it (the blob writes 0x204 here).
    s.pllSelect = (boot->pllSelect & ~(0x200u | 1u << 28)) | 0x4 | 0x200;

    bool singleStage = !pll.twoStages || pll.n2 == pll.m2;
    s.vpll = (boot->vpll & 0xFFF80000u) | (uint32_t)pll.log2P << 16 | (uint32_t)pll.n1 << 8 | pll.m1;
    s.vpllB = (boot->vpllB & 0x7FFF0000u) | 1u << 31 | (pll.twoStages ? (uint32_t)pll.n2 << 8 | pll.m2 : 0);
    s.ramdac580 = boot->ramdac580;
    if (chipVersion > 0x40) {
        s.ramdac580 = singleStage ? s.ramdac580 | 0x100 : s.ramdac580 & ~0x100u;
        if (singleStage) {
            s.vpllB |= 0x011F;
        }
    }
    if (chipVersion > 0x70) {
        s.vpll = (s.vpll & 0x0FFFFFFFu) | (singleStage ? 0x4u : 0xCu) << 28;
    }
}

// nvidiafb's nvGetClocks for the NV40 family: the core (NVPLL) and memory (MPLL) synthesizers of PMC, two stages
// each, except the memory one of the G71 / G73 (0x0290 / 0x0390).
void
NvidiaNv4x::clocks(uint32_t &coreKhz, uint32_t &memoryKhz) const {
    uint32_t p = read32(PMC_MPLL) >> 16 & 7;
    uint32_t pll = read32(PMC_MPLL_COEFFICIENTS);
    uint32_t m = pll & 0xFF;
    uint32_t n = pll >> 8 & 0xFF;
    uint32_t mb = pll >> 16 & 0xFF;
    uint32_t nb = pll >> 24 & 0xFF;
    if ((card.implementation & 0xFFF0) == 0x0290 || (card.implementation & 0xFFF0) == 0x0390) {
        mb = 1;
        nb = 1;
    }
    memoryKhz = m && mb ? (uint32_t)(((uint64_t)n * nb * crystalKhz / (m * mb)) >> p) : 0;

    p = read32(PMC_NVPLL) >> 16 & 7;
    pll = read32(PMC_NVPLL_COEFFICIENTS);
    m = pll & 0xFF;
    n = pll >> 8 & 0xFF;
    mb = pll >> 16 & 0xFF;
    nb = pll >> 24 & 0xFF;
    coreKhz = m && mb ? (uint32_t)(((uint64_t)n * nb * crystalKhz / (m * mb)) >> p) : 0;
}

void
NvidiaNv4x::describeFamily(NvidiaInfo &out) const {
    Format::print(out.familyName, sizeof(out.familyName), "NV4x (Curie: GeForce 6 / 7)");
    out.biosChip = chipVersion;
    if (biosVersion != 0) {
        Format::print(out.biosVersion, sizeof(out.biosVersion), "%02x.%02x.%02x.%02x", biosVersion >> 24,
                      biosVersion >> 16 & 0xFF, biosVersion >> 8 & 0xFF, biosVersion & 0xFF);
    }
    out.twoStagePll = true;
    out.vco1MinKhz = pllLimits.vco1MinKhz;
    out.vco1MaxKhz = pllLimits.vco1MaxKhz;
    out.vco2MinKhz = pllLimits.vco2MinKhz;
    out.vco2MaxKhz = pllLimits.vco2MaxKhz;
    out.vpllB = read32(PRAMDAC_VPLL_B);
}
