#include "drivers/video/nvidiaNv4x/NvidiaNv4x.h"
#include "kernel/KernelLog.h"
#include "kernel/Paging.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Ports.h"

// The extended CRTC registers a mode change writes, in the order nouveau loads them.
static const uint8_t NV4X_EXTENDED[] = {
    0x19, 0x1A, 0x25, 0x28, 0x2D, 0x1C, 0x1B, 0x20, 0x47, 0x9F, 0x39, 0x3B, 0x41, 0x4B, 0x52, 0x42, 0x53, 0x54,
    0x59, 0x85, 0x86,
};

// The I2C buses of the DDC lines (drive register, sense register), as the DCB of the GeForce 6 / 7 BIOS lists them.
static const uint8_t NV4X_DDC_PORTS[][2] = {{0x3F, 0x3E}, {0x37, 0x36}, {0x51, 0x50}};

static const volatile uint8_t *const NV4X_VIDEO_BIOS = (const volatile uint8_t *)0xC0000;
static constexpr size_t NV4X_VIDEO_BIOS_BYTES = 128 * 1024;

// About a microsecond each: a write to the POST diagnostic port, as Reboot::delay does.
static void
nv4xWaitMicroseconds(unsigned microseconds) {
    for (unsigned i = 0; i < microseconds; i++) {
        Ports::outb(0x80, 0);
    }
}

static bool
nv4xSupported(unsigned chipset) {
    // NV40 to G73 with their own memory; the C51 / C61 / MCP6x integrated ones (0x4C, 0x4E, 0x6x) differ.
    return (chipset >= 0x40 && chipset <= 0x47) || chipset == 0x49 || chipset == 0x4A || chipset == 0x4B;
}

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

SvgaDriver *
NvidiaNv4x::probe() {
    Pci::Location at;
    bool first = true;
    while (Pci::findClass(0x03, 0x00, first, at)) {
        first = false;
        if (Pci::read16(at, Pci::VENDOR_ID) != 0x10DE) {
            continue;
        }
        uint16_t device = Pci::read16(at, Pci::DEVICE_ID);
        uint64_t registers;
        uint64_t registersSize;
        uint64_t vram;
        uint64_t vramSize;
        if (!Pci::memoryBar(at, 0, registers, registersSize) || !Pci::memoryBar(at, 1, vram, vramSize) ||
            registersSize < (16u << 20) || registers + registersSize > Paging::mappedBytes() ||
            vram + vramSize > Paging::mappedBytes()) {
            kprintf("Video: NVIDIA 10de:%04x: register or memory BAR unusable\n", (uint32_t)device);
            continue;
        }
        Pci::write16(at, Pci::COMMAND, Pci::read16(at, Pci::COMMAND) | Pci::COMMAND_MEMORY | Pci::COMMAND_IO);
        volatile uint8_t *mmio = (volatile uint8_t *)(uintptr_t)registers;
        uint32_t boot0 = *(volatile uint32_t *)(mmio + PMC_BOOT_0);
        unsigned chipset = (boot0 & 0x1F000000) ? (boot0 & 0x1FF00000) >> 20 : 0;
        if (!nv4xSupported(chipset)) {
            kprintf("Video: NVIDIA 10de:%04x is chipset NV%02x, not a GeForce 6 / 7 (NV4x)\n", (uint32_t)device,
                    chipset);
            continue;
        }
        uint32_t memory = *(volatile uint32_t *)(mmio + PFB_MEMORY_SIZE) & 0xFF000000u;
        uint64_t aperture = memory != 0 && memory < vramSize ? memory : vramSize;
        if (aperture > 0xFFFFF000u) {
            aperture = 0xFFFFF000u;
        }
        kprintf("Video: NVIDIA 10de:%04x, chipset NV%02x rev %02x, %u MB of video memory, %u MB at 0x%x, "
                "registers at 0x%x\n", (uint32_t)device, chipset, boot0 & 0xFF, memory >> 20,
                (uint32_t)(vramSize >> 20), (uint32_t)vram, (uint32_t)registers);
        NvidiaNv4x *driver = new NvidiaNv4x(mmio, (volatile uint8_t *)(uintptr_t)vram, (uint32_t)aperture, chipset);
        if (driver->init()) {
            return driver;
        }
        delete driver;
    }
    return nullptr;
}

NvidiaNv4x::~NvidiaNv4x() {
    delete boot;
    delete mode;
}

bool
NvidiaNv4x::init() {
    crtcWrite(CR_UNLOCK, 0x57);
    uint8_t owner = crtcRead(CR_OWNER);
    if (owner == 3) {
        kprintf("Video: NV4x: the BIOS drives the display from head B, which is not supported\n");
        return false;
    }
    if (owner == 4) {
        // Heads tied: head B repeats head A. Untie them, as nouveau does, so that head A is programmed alone.
        crtcWrite(CR_OWNER, 0);
    }
    if (crtcRead(CR_PIXEL) & 3) {
        kprintf("Video: NV4x: the firmware left a graphics mode (CR28 0x%02x), leaving the card alone\n",
                (uint32_t)crtcRead(CR_PIXEL));
        return false;
    }

    bool fromBios = false;
    const volatile uint8_t *rom = NV4X_VIDEO_BIOS;
    if (rom[0] == 0x55 && rom[1] == 0xAA) {
        uint32_t pcir = (uint32_t)rom[0x18] | (uint32_t)rom[0x19] << 8;
        if (pcir + 8 < NV4X_VIDEO_BIOS_BYTES && rom[pcir] == 'P' && rom[pcir + 1] == 'C' && rom[pcir + 2] == 'I' &&
            rom[pcir + 3] == 'R' && rom[pcir + 4] == 0xDE && rom[pcir + 5] == 0x10) {
            fromBios = Nv4xPll::fromVideoBios(rom, NV4X_VIDEO_BIOS_BYTES, pllLimits, chipVersion);
        }
    }
    if (!fromBios) {
        pllLimits = Nv4xPll::fallback();
        chipVersion = nv4xBiosChip(chipset);
        kprintf("Video: NV4x: no PLL limits in the video BIOS at 0xC0000, using the G73 ones\n");
    }
    if (chipVersion == 0) {
        chipVersion = nv4xBiosChip(chipset);
    }
    switch (read32(PEXTDEV_STRAPS) & 0x00400040) {
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

    uint8_t block[Edid::BLOCK_BYTES];
    for (const uint8_t *port : NV4X_DDC_PORTS) {
        if (readEdid(port[0], port[1], block) && edid.parse(block)) {
            kprintf("Video: NV4x: monitor %s %04x \"%s\" (%s input) on DDC CR%02x\n", edid.vendor(),
                    (uint32_t)edid.product(), edid.name(), edid.analogInput() ? "analog" : "digital",
                    (uint32_t)port[0]);
            if (edid.detailedCount() > 0) {
                const DisplayTiming &t = edid.detailedTiming(0);
                kprintf("Video: NV4x: preferred mode %ux%u at %u Hz, %u kHz pixel clock\n", t.hDisplay, t.vDisplay,
                        t.refreshHz(), t.pixelClockKhz);
            }
            break;
        }
    }
    if (!edid.valid()) {
        kprintf("Video: NV4x: no EDID from the monitor, offering the VESA modes\n");
    }

    boot = new HeadState;
    mode = new HeadState;
    save(*boot);
    logState("boot", *boot);
    return true;
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
          (uint32_t)s.extended[0x2D], (uint32_t)s.extended[0x41], (uint32_t)s.extended[0x42], (uint32_t)crtcRead(CR_OWNER),
          (uint32_t)s.extended[0x47], (uint32_t)s.extended[0x4B], (uint32_t)s.extended[0x52], read32(PLL_CLOCK_GATE));
}

uint8_t
NvidiaNv4x::crtcRead(uint8_t index) {
    write8(PCIO + 0x3D4, index);
    return read8(PCIO + 0x3D5);
}

void
NvidiaNv4x::crtcWrite(uint8_t index, uint8_t value) {
    write8(PCIO + 0x3D4, index);
    write8(PCIO + 0x3D5, value);
}

uint8_t
NvidiaNv4x::seqRead(uint8_t index) {
    write8(PVIO + 0x3C4, index);
    return read8(PVIO + 0x3C5);
}

void
NvidiaNv4x::seqWrite(uint8_t index, uint8_t value) {
    write8(PVIO + 0x3C4, index);
    write8(PVIO + 0x3C5, value);
}

uint8_t
NvidiaNv4x::grRead(uint8_t index) {
    write8(PVIO + 0x3CE, index);
    return read8(PVIO + 0x3CF);
}

void
NvidiaNv4x::grWrite(uint8_t index, uint8_t value) {
    write8(PVIO + 0x3CE, index);
    write8(PVIO + 0x3CF, value);
}

// With bit 5 of the index clear the attribute controller lets the palette be written and blanks the screen: callers
// are inside protect().
uint8_t
NvidiaNv4x::attrRead(uint8_t index) {
    (void)read8(PCIO + 0x3DA);
    write8(PCIO + 0x3C0, index);
    return read8(PCIO + 0x3C1);
}

void
NvidiaNv4x::attrWrite(uint8_t index, uint8_t value) {
    (void)read8(PCIO + 0x3DA);
    write8(PCIO + 0x3C0, index);
    write8(PCIO + 0x3C0, value);
}

void
NvidiaNv4x::protect(bool on) {
    if (on) {
        seqWrite(0, 0x01);
        seqWrite(1, (uint8_t)(seqRead(1) | 0x20));
        (void)read8(PCIO + 0x3DA);
        write8(PCIO + 0x3C0, 0x00);
    } else {
        seqWrite(1, (uint8_t)(seqRead(1) & ~0x20));
        seqWrite(0, 0x03);
        (void)read8(PCIO + 0x3DA);
        write8(PCIO + 0x3C0, 0x20);
    }
}

void
NvidiaNv4x::save(HeadState &s) {
    s.misc = read8(PVIO + 0x3CC);
    for (uint8_t i = 0; i < 5; i++) {
        s.sequencer[i] = seqRead(i);
    }
    for (uint8_t i = 0; i < STANDARD_CRTC; i++) {
        s.crtc[i] = crtcRead(i);
    }
    for (uint8_t i = 0; i < 9; i++) {
        s.graphics[i] = grRead(i);
    }
    for (uint8_t i = 0; i < 21; i++) {
        s.attribute[i] = attrRead(i);
    }
    (void)read8(PCIO + 0x3DA);
    write8(PCIO + 0x3C0, 0x20);
    write8(PDIO + 0x3C6, 0xFF);
    write8(PDIO + 0x3C7, 0x00);
    for (unsigned i = 0; i < sizeof(s.palette); i++) {
        s.palette[i] = read8(PDIO + 0x3C9);
    }
    Memory::set(s.extended, 0, sizeof(s.extended));
    for (uint8_t index : NV4X_EXTENDED) {
        s.extended[index] = crtcRead(index);
    }
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
NvidiaNv4x::load(const HeadState &s) {
    protect(true);
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

    write8(PDIO + 0x3C6, 0xFF);
    write8(PDIO + 0x3C8, 0x00);
    for (unsigned i = 0; i < sizeof(s.palette); i++) {
        write8(PDIO + 0x3C9, s.palette[i]);
    }

    write8(PVIO + 0x3C2, s.misc);
    for (uint8_t i = 1; i < 5; i++) {
        seqWrite(i, s.sequencer[i]);
    }
    crtcWrite(0x11, (uint8_t)(crtcRead(0x11) & 0x7F));
    for (uint8_t i = 0; i < STANDARD_CRTC; i++) {
        crtcWrite(i, i == 0x11 ? (uint8_t)(s.crtc[i] & 0x7F) : s.crtc[i]);
    }
    crtcWrite(0x11, s.crtc[0x11]);
    for (uint8_t i = 0; i < 9; i++) {
        grWrite(i, s.graphics[i]);
    }
    for (uint8_t i = 0; i < 21; i++) {
        attrWrite(i, s.attribute[i]);
    }
    protect(false);
    crtcWrite(CR_SHADOW_LOCK, s.extended[CR_SHADOW_LOCK]);
}

bool
NvidiaNv4x::timingFor(unsigned newWidth, unsigned newHeight, DisplayTiming &out) const {
    const DisplayTiming *timing = edid.valid() ? edid.detailedFor(newWidth, newHeight) : nullptr;
    if (!timing) {
        timing = DisplayTiming::standard(newWidth, newHeight);
        if (!timing || (edid.valid() && !edid.supports(*timing))) {
            return false;
        }
    }
    // The CRTC counts characters of 8 pixels: 9 bits of horizontal total (minus 5), 12 bits of vertical total.
    if (!timing->sane() || timing->hDisplay % 8 != 0 || timing->pixelClockKhz > MAX_PIXEL_CLOCK_KHZ ||
        timing->hTotal / 8 > 0x1FF + 5 || timing->vTotal > 0xFFF + 2) {
        return false;
    }
    out = *timing;
    return true;
}

bool
NvidiaNv4x::coefficientsFor(uint32_t khz, Nv4xPll::Coefficients &out) const {
    // Below half the first VCO's maximum, newer NV4x run the VPLL on its first stage only, as the blob does.
    bool twoStages = !(chipset > 0x40 && khz <= pllLimits.vco1MaxKhz / 2);
    if (!Nv4xPll::calculate(pllLimits, crystalKhz, chipVersion, khz, twoStages, out)) {
        return false;
    }
    uint32_t error = out.khz > khz ? out.khz - khz : khz - out.khz;
    return error <= khz / 200;
}

bool
NvidiaNv4x::canSet(unsigned newWidth, unsigned newHeight, unsigned bytesPerPixel) const {
    if (bytesPerPixel < 1 || bytesPerPixel > 4) {
        return false;
    }
    DisplayTiming timing;
    if (!timingFor(newWidth, newHeight, timing)) {
        return false;
    }
    unsigned scan = bytesPerPixel == 3 ? 4 : bytesPerPixel;
    uint32_t newPitch = (newWidth * scan + PITCH_ALIGN - 1) / PITCH_ALIGN * PITCH_ALIGN;
    if ((uint64_t)newPitch * newHeight > apertureBytes || newPitch / 8 > 0xFFF) {
        return false;
    }
    Nv4xPll::Coefficients pll;
    return coefficientsFor(timing.pixelClockKhz, pll);
}

void
NvidiaNv4x::compute(const DisplayTiming &t, unsigned bytes, uint32_t newPitch, const Nv4xPll::Coefficients &pll,
                    HeadState &s) {
    s = *boot;
    unsigned hDisplay = t.hDisplay / 8u - 1;
    unsigned hStart = t.hSyncStart / 8u + 1;
    unsigned hEnd = t.hSyncEnd / 8u + 1;
    unsigned hTotal = t.hTotal / 8u - 5;
    unsigned hBlankStart = t.hDisplay / 8u - 1;
    unsigned hBlankEnd = t.hTotal / 8u - 1;
    unsigned vDisplay = t.vDisplay - 1u;
    unsigned vStart = t.vSyncStart - 1u;
    unsigned vEnd = t.vSyncEnd - 1u;
    unsigned vTotal = t.vTotal - 2u;
    unsigned vBlankStart = t.vDisplay - 1u;
    unsigned vBlankEnd = t.vTotal - 1u;
    unsigned offset = newPitch / 8;

    s.misc = (uint8_t)(0x23 | (t.hSyncPositive ? 0 : 0x40) | (t.vSyncPositive ? 0 : 0x80));
    s.sequencer[0] = 0x03;
    s.sequencer[1] = 0x21;
    s.sequencer[2] = 0x0F;
    s.sequencer[3] = 0x00;
    s.sequencer[4] = 0x0E;

    uint8_t *cr = s.crtc;
    cr[0x00] = (uint8_t)hTotal;
    cr[0x01] = (uint8_t)hDisplay;
    cr[0x02] = (uint8_t)hBlankStart;
    cr[0x03] = (uint8_t)(0x80 | (hBlankEnd & 0x1F));
    cr[0x04] = (uint8_t)hStart;
    cr[0x05] = (uint8_t)((hBlankEnd >> 5 & 1) << 7 | (hEnd & 0x1F));
    cr[0x06] = (uint8_t)vTotal;
    cr[0x07] = (uint8_t)((vStart >> 9 & 1) << 7 | (vDisplay >> 9 & 1) << 6 | (vTotal >> 9 & 1) << 5 | 1 << 4 |
                         (vBlankStart >> 8 & 1) << 3 | (vStart >> 8 & 1) << 2 | (vDisplay >> 8 & 1) << 1 |
                         (vTotal >> 8 & 1));
    cr[0x08] = 0x00;
    cr[0x09] = (uint8_t)(1 << 6 | (vBlankStart >> 9 & 1) << 5);
    for (unsigned i = 0x0A; i <= 0x0F; i++) {
        cr[i] = 0x00;
    }
    cr[0x10] = (uint8_t)vStart;
    // Bit 7 locks CR0-7 again once they are written.
    cr[0x11] = (uint8_t)(0x80 | 1 << 5 | (vEnd & 0x0F));
    cr[0x12] = (uint8_t)vDisplay;
    cr[0x13] = (uint8_t)offset;
    cr[0x14] = 0x00;
    cr[0x15] = (uint8_t)vBlankStart;
    cr[0x16] = (uint8_t)vBlankEnd;
    cr[0x17] = 0xC3;
    cr[0x18] = 0xFF;

    static const uint8_t GRAPHICS[9] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x05, 0x0F, 0xFF};
    for (unsigned i = 0; i < 9; i++) {
        s.graphics[i] = GRAPHICS[i];
    }
    for (unsigned i = 0; i < 16; i++) {
        s.attribute[i] = (uint8_t)i;
    }
    s.attribute[0x10] = 0x01;
    s.attribute[0x11] = 0x00;
    s.attribute[0x12] = 0x0F;
    s.attribute[0x13] = 0x00;
    s.attribute[0x14] = 0x00;

    uint8_t *ex = s.extended;
    ex[0x19] = (uint8_t)((offset >> 8 & 7) << 5);
    ex[0x42] = (uint8_t)((offset >> 11 & 1) << 6);
    ex[0x1A] = (uint8_t)(t.hDisplay < 1280 ? 0x04 : 0x00);
    ex[0x25] = (uint8_t)((hBlankEnd >> 6 & 1) << 4 | (vBlankStart >> 10 & 1) << 3 | (vStart >> 10 & 1) << 2 |
                         (vDisplay >> 10 & 1) << 1 | (vTotal >> 10 & 1));
    ex[0x2D] = (uint8_t)((hStart >> 8 & 1) << 3 | (hBlankStart >> 8 & 1) << 2 | (hDisplay >> 8 & 1) << 1 |
                         (hTotal >> 8 & 1));
    ex[0x41] = (uint8_t)((vBlankStart >> 11 & 1) << 6 | (vStart >> 11 & 1) << 4 | (vDisplay >> 11 & 1) << 2 |
                         (vTotal >> 11 & 1));
    ex[0x39] = 0xFF;
    ex[0x1C] = (uint8_t)(boot->extended[0x1C] & ~0x20);
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
    ex[CR_PIXEL] = (uint8_t)(bytes == 1 ? 1 : bytes == 2 ? 2 : 3);
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

    if (bytes == 1) {
        Memory::copy(s.palette, boot->palette, sizeof(s.palette));
    } else {
        for (unsigned i = 0; i < 256; i++) {
            s.palette[3 * i] = (uint8_t)i;
            s.palette[3 * i + 1] = (uint8_t)i;
            s.palette[3 * i + 2] = (uint8_t)i;
        }
    }
}

bool
NvidiaNv4x::setMode(unsigned newWidth, unsigned newHeight, unsigned bytesPerPixel) {
    if (!canSet(newWidth, newHeight, bytesPerPixel)) {
        return false;
    }
    DisplayTiming timing;
    Nv4xPll::Coefficients pll;
    timingFor(newWidth, newHeight, timing);
    coefficientsFor(timing.pixelClockKhz, pll);
    unsigned scan = bytesPerPixel == 3 ? 4 : bytesPerPixel;
    uint32_t newPitch = (newWidth * scan + PITCH_ALIGN - 1) / PITCH_ALIGN * PITCH_ALIGN;

    compute(timing, scan, newPitch, pll, *mode);
    logState("computed", *mode);
    load(*mode);
    HeadState *readBack = new HeadState;
    save(*readBack);
    logState("loaded", *readBack);
    delete readBack;
    pitch = newPitch;
    modeBytes = bytesPerPixel;
    scanBytes = scan;
    width = newWidth;
    height = newHeight;
    volatile uint32_t *words = (volatile uint32_t *)frame;
    size_t count = (size_t)pitch * height / 4;
    for (size_t i = 0; i < count; i++) {
        words[i] = 0;
    }
    // VirtualConsole::show calls this with its lock held, so kprintf (which prints there) would deadlock.
    klogf("Video: NV4x: %ux%u at %u Hz, %u bits per pixel, pixel clock %u kHz (N %u M %u N2 %u M2 %u P %u)\n",
            newWidth, newHeight, timing.refreshHz(), scan * 8, pll.khz, (uint32_t)pll.n1, (uint32_t)pll.m1,
            (uint32_t)pll.n2, (uint32_t)pll.m2, 1u << pll.log2P);
    return true;
}

void
NvidiaNv4x::leaveMode() {
    if (modeBytes == 0) {
        return;
    }
    load(*boot);
    modeBytes = 0;
    HeadState *readBack = new HeadState;
    save(*readBack);
    logState("restored", *readBack);
    delete readBack;
}

void
NvidiaNv4x::writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) {
    if (x >= width || y >= height || sourceBytesPerPixel != modeBytes) {
        return;
    }
    volatile uint8_t *row = frame + (size_t)y * pitch;
    switch (scanBytes) {
    case 1:
        row[x] = (uint8_t)color;
        break;
    case 2:
        ((volatile uint16_t *)row)[x] = (uint16_t)color;
        break;
    default:
        ((volatile uint32_t *)row)[x] = color & 0xFFFFFFu;
        break;
    }
}

void
NvidiaNv4x::writeSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count) {
    if (y >= height || x >= width || bytesPerPixel != modeBytes) {
        return;
    }
    if (count > width - x) {
        count = width - x;
    }
    volatile uint8_t *row = frame + (size_t)y * pitch;
    if (modeBytes != 3) {
        Memory::copy((void *)(row + (size_t)x * modeBytes), pixels, (size_t)count * modeBytes);
        return;
    }
    volatile uint32_t *dst = (volatile uint32_t *)row + x;
    for (unsigned i = 0; i < count; i++) {
        const uint8_t *p = pixels + 3 * i;
        dst[i] = (uint32_t)p[0] | (uint32_t)p[1] << 8 | (uint32_t)p[2] << 16;
    }
}

void
NvidiaNv4x::ddcDrive(uint8_t drive, uint8_t bit, bool high) {
    uint8_t value = crtcRead(drive);
    value = high ? (uint8_t)(value | bit) : (uint8_t)(value & ~bit);
    crtcWrite(drive, (uint8_t)(value | 0x01));
}

bool
NvidiaNv4x::ddcSense(uint8_t sense, uint8_t bit) {
    return (crtcRead(sense) & bit) != 0;
}

// Releases SCL and waits for it to go high: a slave may hold it low (clock stretching).
bool
NvidiaNv4x::ddcClockHigh(uint8_t drive, uint8_t sense) {
    ddcDrive(drive, 0x20, true);
    for (unsigned i = 0; i < 1000; i++) {
        if (ddcSense(sense, 0x04)) {
            nv4xWaitMicroseconds(5);
            return true;
        }
        nv4xWaitMicroseconds(1);
    }
    return false;
}

void
NvidiaNv4x::ddcStart(uint8_t drive, uint8_t sense) {
    ddcDrive(drive, 0x10, true);
    ddcClockHigh(drive, sense);
    ddcDrive(drive, 0x10, false);
    nv4xWaitMicroseconds(5);
    ddcDrive(drive, 0x20, false);
    nv4xWaitMicroseconds(5);
}

void
NvidiaNv4x::ddcStop(uint8_t drive, uint8_t sense) {
    ddcDrive(drive, 0x10, false);
    nv4xWaitMicroseconds(5);
    ddcClockHigh(drive, sense);
    ddcDrive(drive, 0x10, true);
    nv4xWaitMicroseconds(5);
}

bool
NvidiaNv4x::ddcWrite(uint8_t drive, uint8_t sense, uint8_t byte) {
    for (int bit = 7; bit >= 0; bit--) {
        ddcDrive(drive, 0x10, (byte >> bit & 1) != 0);
        nv4xWaitMicroseconds(2);
        if (!ddcClockHigh(drive, sense)) {
            return false;
        }
        ddcDrive(drive, 0x20, false);
        nv4xWaitMicroseconds(2);
    }
    ddcDrive(drive, 0x10, true);
    nv4xWaitMicroseconds(2);
    if (!ddcClockHigh(drive, sense)) {
        return false;
    }
    bool acknowledged = !ddcSense(sense, 0x08);
    ddcDrive(drive, 0x20, false);
    nv4xWaitMicroseconds(2);
    return acknowledged;
}

uint8_t
NvidiaNv4x::ddcRead(uint8_t drive, uint8_t sense, bool acknowledge) {
    uint8_t byte = 0;
    ddcDrive(drive, 0x10, true);
    for (int bit = 7; bit >= 0; bit--) {
        nv4xWaitMicroseconds(2);
        ddcClockHigh(drive, sense);
        if (ddcSense(sense, 0x08)) {
            byte = (uint8_t)(byte | 1 << bit);
        }
        ddcDrive(drive, 0x20, false);
    }
    ddcDrive(drive, 0x10, !acknowledge);
    nv4xWaitMicroseconds(2);
    ddcClockHigh(drive, sense);
    ddcDrive(drive, 0x20, false);
    ddcDrive(drive, 0x10, true);
    nv4xWaitMicroseconds(2);
    return byte;
}

bool
NvidiaNv4x::readEdid(uint8_t drive, uint8_t sense, uint8_t *block) {
    uint8_t saved = crtcRead(drive);
    ddcDrive(drive, 0x30, true);
    nv4xWaitMicroseconds(10);
    bool ok = false;
    // Lines that do not float high have no bus (or a stuck one) behind them.
    if (ddcSense(sense, 0x04) && ddcSense(sense, 0x08)) {
        ddcStart(drive, sense);
        if (ddcWrite(drive, sense, Edid::DDC_ADDRESS << 1) && ddcWrite(drive, sense, 0x00)) {
            ddcStart(drive, sense);
            if (ddcWrite(drive, sense, (uint8_t)(Edid::DDC_ADDRESS << 1 | 1))) {
                for (unsigned i = 0; i < Edid::BLOCK_BYTES; i++) {
                    block[i] = ddcRead(drive, sense, i + 1 < Edid::BLOCK_BYTES);
                }
                ok = true;
            }
        }
        ddcStop(drive, sense);
    }
    crtcWrite(drive, saved);
    return ok;
}
