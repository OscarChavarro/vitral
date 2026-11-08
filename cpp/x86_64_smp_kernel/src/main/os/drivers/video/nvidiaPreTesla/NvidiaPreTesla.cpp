#include "drivers/video/nvidiaPreTesla/NvidiaPreTesla.h"
#include "drivers/video/nvidiaPreTesla/NvidiaNv04.h"
#include "drivers/video/nvidiaPreTesla/NvidiaNv4x.h"
#include "drivers/video/nvidiaPreTesla2d/NvidiaPreTesla2d.h"
#include "drivers/video/svga/Svga.h"
#include "fs/Err.h"
#include "kernel/KernelLog.h"
#include "kernel/Paging.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Ports.h"

NvidiaPreTesla *NvidiaPreTesla::probed = nullptr;

static constexpr uint8_t NV_PCI_REVISION = 0x08;
static constexpr uint8_t NV_PCI_SUBSYSTEM_VENDOR = 0x2C;
static constexpr uint8_t NV_PCI_SUBSYSTEM = 0x2E;
static constexpr uint32_t NV_PFB_BOOT_0 = 0x100000;
static constexpr uint32_t NV_PFB_MEMORY_SIZE = 0x10020C;
static constexpr uint32_t NV_PBUS_PCI_ID = 0x001800;

// The chipset number nouveau gives a PMC_BOOT_0 value: NV04 and NV05 report theirs only in older fields.
static unsigned
nvPreTeslaChipset(uint32_t boot0) {
    if (boot0 & 0x1F000000) {
        return (boot0 & 0x1FF00000) >> 20;
    }
    if ((boot0 & 0xFF00FFF0) == 0x20004000) {
        return (boot0 & 0x00F00000) ? 0x05 : 0x04;
    }
    return 0x03;
}

static bool
nvPreTeslaNv4x(unsigned chipset) {
    // NV40 to G73 with their own memory; the C51 / C61 / MCP6x integrated ones (0x4C, 0x4E, 0x6x) differ.
    return (chipset >= 0x40 && chipset <= 0x47) || chipset == 0x49 || chipset == 0x4A || chipset == 0x4B;
}

// The memory of a TNT / TNT2, from the strap bits of PFB_BOOT_0 (nvidiafb's nv4GetConfig).
static uint32_t
nvPreTeslaNv04Memory(uint32_t boot) {
    if (boot & 0x100) {
        return (((boot >> 12) & 0x0F) * 2 + 2) << 20;
    }
    switch (boot & 3) {
    case 0:
        return 32u << 20;
    case 1:
        return 4u << 20;
    case 2:
        return 8u << 20;
    default:
        return 16u << 20;
    }
}

SvgaDriver *
NvidiaPreTesla::probe() {
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
        unsigned chipset = nvPreTeslaChipset(boot0);
        bool nv04 = chipset == 0x04 || chipset == 0x05;
        // 0x00A0, the Aladdin TNT2, keeps its frame buffer in system memory.
        if ((!nv04 && !nvPreTeslaNv4x(chipset)) || device == 0x00A0) {
            kprintf("Video: NVIDIA 10de:%04x is chipset NV%02x: only the RIVA TNT / TNT2 (NV04 / NV05) and the "
                    "GeForce 6 / 7 (NV4x) are supported\n", (uint32_t)device, chipset);
            continue;
        }
        Card card;
        card.at = at;
        card.device = device;
        card.implementation = device;
        // Behind the AGP to PCI Express bridge of the AGP GeForce 6 / 7 the GPU's own ID is in its PCI mirror.
        if ((device & 0xFFF0) == 0x00F0 || (device & 0xFFF0) == 0x02E0) {
            uint32_t id = *(volatile uint32_t *)(mmio + NV_PBUS_PCI_ID);
            if ((id & 0xFFFF) == 0x10DE) {
                card.implementation = (uint16_t)(id >> 16);
            } else if ((id & 0xFFFF0000u) == 0xDE100000u) {
                card.implementation = (uint16_t)(((id << 8) & 0xFF00) | ((id >> 8) & 0x00FF));
            }
        }
        card.subsystemVendor = Pci::read16(at, NV_PCI_SUBSYSTEM_VENDOR);
        card.subsystemDevice = Pci::read16(at, NV_PCI_SUBSYSTEM);
        card.revision = Pci::read8(at, NV_PCI_REVISION);
        card.chipset = chipset;
        card.boot0 = boot0;
        card.mmio = mmio;
        card.registersAddress = registers;
        card.registersBytes = registersSize;
        card.frame = (volatile uint8_t *)(uintptr_t)vram;
        card.frameAddress = vram;
        card.frameBytes = vramSize;
        card.memoryBytes = nv04 ? nvPreTeslaNv04Memory(*(volatile uint32_t *)(mmio + NV_PFB_BOOT_0))
                                : *(volatile uint32_t *)(mmio + NV_PFB_MEMORY_SIZE) & 0xFF000000u;
        uint64_t aperture = card.memoryBytes != 0 && card.memoryBytes < vramSize ? card.memoryBytes : vramSize;
        if (aperture > 0xFFFFF000u) {
            aperture = 0xFFFFF000u;
        }
        card.apertureBytes = (uint32_t)aperture;
        kprintf("Video: NVIDIA 10de:%04x (%04x), chipset NV%02x rev %02x, %u MB of video memory, %u MB at 0x%x, "
                "registers at 0x%x\n", (uint32_t)device, (uint32_t)card.implementation, chipset, boot0 & 0xFF,
                card.memoryBytes >> 20, (uint32_t)(vramSize >> 20), (uint32_t)vram, (uint32_t)registers);
        NvidiaPreTesla *driver = nv04 ? (NvidiaPreTesla *)new NvidiaNv04(card) : (NvidiaPreTesla *)new NvidiaNv4x(card);
        if (driver->init()) {
            probed = driver;
            return driver;
        }
        delete driver;
    }
    return nullptr;
}

NvidiaPreTesla::NvidiaPreTesla(const Card &found)
    : card(found), crystalKhz(0), edidPort(0), pitch(0), modeBytes(0), scanBytes(0), width(0), height(0),
      modePll(), boot(nullptr), mode(nullptr), engine(nullptr), accelerating(false) {
    chipName[0] = 0;
}

NvidiaPreTesla::~NvidiaPreTesla() {
    if (probed == this) {
        probed = nullptr;
    }
    delete engine;
    delete boot;
    delete mode;
}

// "NVIDIA <marketing name> (<chip>)": by PCI ID for the NV04 family, whose chipset number is shared, by chipset for
// the GeForce 6 / 7.
void
NvidiaPreTesla::nameChip() {
    const char *model = nullptr;
    if (card.chipset <= 0x05) {
        switch (card.device) {
        case 0x0020:
            model = "RIVA TNT (NV04)";
            break;
        case 0x0029:
            model = "RIVA TNT2 Ultra (NV05)";
            break;
        case 0x002C:
        case 0x002E:
        case 0x002F:
            model = "Vanta (NV05)";
            break;
        case 0x002D:
            model = "RIVA TNT2 Model 64 (NV05)";
            break;
        default:
            model = card.chipset == 0x04 ? "RIVA TNT (NV04)" : "RIVA TNT2 (NV05)";
            break;
        }
    } else {
        switch (card.chipset) {
        case 0x40:
            model = "GeForce 6800 (NV40)";
            break;
        case 0x41:
        case 0x42:
        case 0x45:
            model = "GeForce 6800 (NV41 / NV42 / NV45)";
            break;
        case 0x43:
            model = "GeForce 6600 (NV43)";
            break;
        case 0x44:
        case 0x4A:
            model = "GeForce 6200 (NV44)";
            break;
        case 0x46:
            model = "GeForce 7300 / 7400 (G72)";
            break;
        case 0x47:
            model = "GeForce 7800 (G70)";
            break;
        case 0x49:
            model = "GeForce 7900 (G71)";
            break;
        default:
            model = "GeForce 7600 (G73)";
            break;
        }
    }
    Format::print(chipName, sizeof(chipName), "NVIDIA %s", model);
}

bool
NvidiaPreTesla::init() {
    nameChip();
    crtcWrite(CR_UNLOCK, 0x57);
    if (!initHead()) {
        return false;
    }
    if (crtcRead(CR_PIXEL) & 3) {
        kprintf("Video: %s: the firmware left a graphics mode (CR28 0x%02x), leaving the card alone\n", chipName,
                (uint32_t)crtcRead(CR_PIXEL));
        return false;
    }
    initClocks();

    unsigned ports = 0;
    const uint8_t (*port)[2] = ddcPorts(ports);
    uint8_t block[Edid::BLOCK_BYTES];
    for (unsigned i = 0; i < ports; i++) {
        if (readEdid(port[i][0], port[i][1], block) && edid.parse(block)) {
            edidPort = port[i][0];
            kprintf("Video: %s: monitor %s %04x \"%s\" (%s input) on DDC CR%02x\n", familyName(), edid.vendor(),
                    (uint32_t)edid.product(), edid.name(), edid.analogInput() ? "analog" : "digital",
                    (uint32_t)edidPort);
            if (edid.detailedCount() > 0) {
                const DisplayTiming &t = edid.detailedTiming(0);
                kprintf("Video: %s: preferred mode %ux%u at %u Hz, %u kHz pixel clock\n", familyName(), t.hDisplay,
                        t.vDisplay, t.refreshHz(), t.pixelClockKhz);
            }
            break;
        }
    }
    if (!edid.valid()) {
        kprintf("Video: %s: no EDID from the monitor, offering the VESA modes\n", familyName());
    }

    boot = new HeadState;
    mode = new HeadState;
    engine = new NvidiaPreTesla2d(*this);
    save(*boot);
    logState("boot", *boot);
    return true;
}

// About a microsecond each: a write to the POST diagnostic port, as Reboot::delay does.
void
NvidiaPreTesla::waitMicroseconds(unsigned microseconds) {
    for (unsigned i = 0; i < microseconds; i++) {
        Ports::outb(0x80, 0);
    }
}

uint32_t
NvidiaPreTesla::mapBytes() const {
    return card.apertureBytes < ENGINE_MAP_LIMIT ? card.apertureBytes : ENGINE_MAP_LIMIT;
}

uint32_t
NvidiaPreTesla::usableBytes() const {
    return mapBytes() - engineReserveBytes();
}

uint8_t
NvidiaPreTesla::crtcRead(uint8_t index) const {
    *(volatile uint8_t *)(card.mmio + PCIO + 0x3D4) = index;
    return read8(PCIO + 0x3D5);
}

void
NvidiaPreTesla::crtcWrite(uint8_t index, uint8_t value) {
    write8(PCIO + 0x3D4, index);
    write8(PCIO + 0x3D5, value);
}

uint8_t
NvidiaPreTesla::seqRead(uint8_t index) const {
    *(volatile uint8_t *)(card.mmio + PVIO + 0x3C4) = index;
    return read8(PVIO + 0x3C5);
}

void
NvidiaPreTesla::seqWrite(uint8_t index, uint8_t value) {
    write8(PVIO + 0x3C4, index);
    write8(PVIO + 0x3C5, value);
}

uint8_t
NvidiaPreTesla::grRead(uint8_t index) const {
    *(volatile uint8_t *)(card.mmio + PVIO + 0x3CE) = index;
    return read8(PVIO + 0x3CF);
}

void
NvidiaPreTesla::grWrite(uint8_t index, uint8_t value) {
    write8(PVIO + 0x3CE, index);
    write8(PVIO + 0x3CF, value);
}

// With bit 5 of the index clear the attribute controller lets the palette be written and blanks the screen: callers
// are inside protect().
uint8_t
NvidiaPreTesla::attrRead(uint8_t index) const {
    (void)read8(PCIO + 0x3DA);
    *(volatile uint8_t *)(card.mmio + PCIO + 0x3C0) = index;
    return read8(PCIO + 0x3C1);
}

void
NvidiaPreTesla::attrWrite(uint8_t index, uint8_t value) {
    (void)read8(PCIO + 0x3DA);
    write8(PCIO + 0x3C0, index);
    write8(PCIO + 0x3C0, value);
}

void
NvidiaPreTesla::protect(bool on) {
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
NvidiaPreTesla::save(HeadState &s) {
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
    unsigned count = 0;
    const uint8_t *extended = extendedRegisters(count);
    for (unsigned i = 0; i < count; i++) {
        s.extended[extended[i]] = crtcRead(extended[i]);
    }
    saveFamily(s);
}

void
NvidiaPreTesla::load(const HeadState &s) {
    protect(true);
    loadFamily(s);

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
    finishLoad(s);
}

bool
NvidiaPreTesla::timingFor(unsigned newWidth, unsigned newHeight, DisplayTiming &out) const {
    const DisplayTiming *timing = edid.valid() ? edid.detailedFor(newWidth, newHeight) : nullptr;
    if (!timing) {
        timing = DisplayTiming::standard(newWidth, newHeight);
        if (!timing || (edid.valid() && !edid.supports(*timing))) {
            return false;
        }
    }
    // The CRTC counts characters of 8 pixels: 9 bits of horizontal total (minus 5), 12 bits of vertical total.
    if (!timing->sane() || timing->hDisplay % 8 != 0 || timing->pixelClockKhz > maxPixelClockKhz() ||
        timing->hTotal / 8 > 0x1FF + 5 || timing->vTotal > 0xFFF + 2) {
        return false;
    }
    out = *timing;
    return true;
}

bool
NvidiaPreTesla::canSet(unsigned newWidth, unsigned newHeight, unsigned bytesPerPixel) const {
    if (bytesPerPixel < 1 || bytesPerPixel > 4) {
        return false;
    }
    DisplayTiming timing;
    if (!timingFor(newWidth, newHeight, timing)) {
        return false;
    }
    unsigned scan = bytesPerPixel == 3 ? 4 : bytesPerPixel;
    uint32_t newPitch = (newWidth * scan + PITCH_ALIGN - 1) / PITCH_ALIGN * PITCH_ALIGN;
    if ((uint64_t)newPitch * newHeight > usableBytes() || newPitch / 8 > maxOffset()) {
        return false;
    }
    NvidiaPll::Coefficients pll;
    return coefficientsFor(timing.pixelClockKhz, pll);
}

// The VGA registers of a linear mode and the extended CRTC registers every family has (nvidiafb's nvidia_calc_regs,
// nouveau's nv_crtc_mode_set_vga): the overflow bits of the timings, the pitch, the pixel depth.
void
NvidiaPreTesla::computeStandard(const DisplayTiming &t, unsigned bytes, uint32_t newPitch, HeadState &s) {
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
    ex[0x1A] = (uint8_t)(t.hDisplay < 1280 ? 0x04 : 0x00);
    ex[0x25] = (uint8_t)((hBlankEnd >> 6 & 1) << 4 | (vBlankStart >> 10 & 1) << 3 | (vStart >> 10 & 1) << 2 |
                         (vDisplay >> 10 & 1) << 1 | (vTotal >> 10 & 1));
    ex[0x2D] = (uint8_t)((hStart >> 8 & 1) << 3 | (hBlankStart >> 8 & 1) << 2 | (hDisplay >> 8 & 1) << 1 |
                         (hTotal >> 8 & 1));
    ex[0x39] = 0xFF;
    ex[0x1C] = (uint8_t)(boot->extended[0x1C] & ~0x20);
    ex[CR_PIXEL] = (uint8_t)(bytes == 1 ? 1 : bytes == 2 ? 2 : 3);

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
NvidiaPreTesla::setMode(unsigned newWidth, unsigned newHeight, unsigned bytesPerPixel) {
    if (!canSet(newWidth, newHeight, bytesPerPixel)) {
        return false;
    }
    if (accelerating) {
        engine->idle();
    }
    DisplayTiming timing;
    NvidiaPll::Coefficients pll;
    timingFor(newWidth, newHeight, timing);
    coefficientsFor(timing.pixelClockKhz, pll);
    unsigned scan = bytesPerPixel == 3 ? 4 : bytesPerPixel;
    uint32_t newPitch = (newWidth * scan + PITCH_ALIGN - 1) / PITCH_ALIGN * PITCH_ALIGN;

    computeStandard(timing, scan, newPitch, *mode);
    computeFamily(timing, scan, newPitch, pll, *mode);
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
    modePll = pll;
    // No CPU clear: it would write the whole frame buffer with interrupts off (Svga::Exclusive), and the caller paints
    // every pixel of the screen next anyway (VirtualConsole::show). The engine clears it without delaying anything.
    if (accelerating) {
        engine->setSurface();
        engine->clear();
    }
    // VirtualConsole::show calls this with its lock held, so kprintf (which prints there) would deadlock.
    klogf("Video: %s: %ux%u at %u Hz, %u bits per pixel, pixel clock %u kHz (N %u M %u N2 %u M2 %u P %u)%s\n",
          familyName(), newWidth, newHeight, timing.refreshHz(), scan * 8, pll.khz, (uint32_t)pll.n1,
          (uint32_t)pll.m1, (uint32_t)pll.n2, (uint32_t)pll.m2, 1u << pll.log2P,
          accelerating ? ", 2D engine on" : "");
    return true;
}

void
NvidiaPreTesla::leaveMode() {
    if (modeBytes == 0) {
        return;
    }
    if (accelerating) {
        engine->idle();
    }
    load(*boot);
    modeBytes = 0;
    width = 0;
    height = 0;
    HeadState *readBack = new HeadState;
    save(*readBack);
    logState("restored", *readBack);
    delete readBack;
}

void
NvidiaPreTesla::writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) {
    if (x >= width || y >= height || sourceBytesPerPixel != modeBytes) {
        return;
    }
    volatile uint8_t *row = card.frame + (size_t)y * pitch;
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
NvidiaPreTesla::writeSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count) {
    if (y >= height || x >= width || bytesPerPixel != modeBytes) {
        return;
    }
    if (count > width - x) {
        count = width - x;
    }
    volatile uint8_t *row = card.frame + (size_t)y * pitch;
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
NvidiaPreTesla::fillRect(unsigned x, unsigned y, unsigned w, unsigned h, unsigned bytesPerPixel, uint32_t color) {
    if (x >= width || y >= height || bytesPerPixel != modeBytes) {
        return;
    }
    if (w > width - x) {
        w = width - x;
    }
    if (h > height - y) {
        h = height - y;
    }
    for (unsigned row = 0; row < h; row++) {
        volatile uint8_t *line = card.frame + (size_t)(y + row) * pitch;
        switch (scanBytes) {
        case 1:
            for (unsigned i = 0; i < w; i++) {
                line[x + i] = (uint8_t)color;
            }
            break;
        case 2:
            for (unsigned i = 0; i < w; i++) {
                ((volatile uint16_t *)line)[x + i] = (uint16_t)color;
            }
            break;
        default:
            for (unsigned i = 0; i < w; i++) {
                ((volatile uint32_t *)line)[x + i] = color & 0xFFFFFFu;
            }
            break;
        }
    }
}

const SvgaPrimitives *
NvidiaPreTesla::startAcceleration() {
    if (!engine->start()) {
        return nullptr;
    }
    if (modeBytes != 0) {
        engine->setSurface();
    }
    accelerating = true;
    return engine->primitives();
}

void
NvidiaPreTesla::stopAcceleration() {
    engine->idle();
    accelerating = false;
}

int64_t
NvidiaPreTesla::control(uint32_t operation, NvidiaInfo *out) {
    NvidiaPreTesla *nvidia = probed;
    if (!nvidia) {
        return -Err::NODEV;
    }
    switch (operation) {
    case NvidiaInfo::DESCRIBE:
        if (!out) {
            return -Err::FAULT;
        }
        {
            Svga::Exclusive guard;
            nvidia->describe(*out);
        }
        return 0;
    case NvidiaInfo::ACCELERATION_ON:
        return Svga::setAccelerated(true) ? 0 : -Err::IO;
    case NvidiaInfo::ACCELERATION_OFF:
        Svga::setAccelerated(false);
        return 0;
    case NvidiaInfo::TEST:
        if (!out) {
            return -Err::FAULT;
        }
        {
            Svga::Exclusive guard;
            nvidia->describe(*out);
            nvidia->test(out->test);
        }
        return 0;
    default:
        return -Err::INVAL;
    }
}

bool
NvidiaPreTesla::test(NvidiaInfo::Test &out) {
    return engine->selfTest(out);
}

// The BIOS version as the BIOS prints it: the four bytes of the BIT 'i' table on the GeForce 6 / 7, the text after
// "Version " in older BIOS images (describeFamily() fills it for the GeForce 6 / 7).
static void
nvPreTeslaBiosText(char *out, unsigned size) {
    static const char KEY[] = "Version ";
    const volatile uint8_t *rom = (const volatile uint8_t *)0xC0000;
    out[0] = 0;
    if (rom[0] != 0x55 || rom[1] != 0xAA) {
        return;
    }
    size_t bytes = (size_t)rom[2] * 512;
    for (size_t at = 0; at + sizeof(KEY) + 1 < bytes; at++) {
        unsigned matched = 0;
        while (matched + 1 < sizeof(KEY) && rom[at + matched] == (uint8_t)KEY[matched]) {
            matched++;
        }
        if (matched + 1 != sizeof(KEY)) {
            continue;
        }
        unsigned length = 0;
        for (size_t c = at + matched; c < bytes && length + 1 < size; c++) {
            uint8_t ch = rom[c];
            if (ch < 0x20 || ch > 0x7E) {
                break;
            }
            out[length++] = (char)ch;
        }
        out[length] = 0;
        return;
    }
}

void
NvidiaPreTesla::describe(NvidiaInfo &out) const {
    Memory::set(&out, 0, sizeof(out));
    Memory::copy(out.chipName, chipName, sizeof(out.chipName));
    out.pciVendor = 0x10DE;
    out.pciDevice = card.device;
    out.implementation = card.implementation;
    out.subsystemVendor = card.subsystemVendor;
    out.subsystemDevice = card.subsystemDevice;
    out.bus = card.at.bus;
    out.slot = card.at.device;
    out.function = card.at.function;
    out.revision = card.revision;
    out.chipset = (uint16_t)card.chipset;
    out.boot0 = card.boot0;
    out.straps = read32(PEXTDEV_BOOT_0);
    out.registersAddress = card.registersAddress;
    out.registersBytes = card.registersBytes;
    out.frameAddress = card.frameAddress;
    out.frameBytes = card.frameBytes;
    out.memoryBytes = card.memoryBytes;
    out.memoryBusBits = memoryBusBits();
    out.mapBytes = mapBytes();
    out.usableBytes = usableBytes();
    out.crystalKhz = crystalKhz;
    clocks(out.coreKhz, out.memoryKhz);
    out.maxPixelKhz = maxPixelClockKhz();
    nvPreTeslaBiosText(out.biosVersion, sizeof(out.biosVersion));

    out.monitor = edid.valid();
    if (out.monitor) {
        Memory::copy(out.monitorVendor, edid.vendor(), sizeof(out.monitorVendor));
        out.monitorProduct = edid.product();
        Format::print(out.monitorName, sizeof(out.monitorName), "%s", edid.name());
        out.monitorAnalog = edid.analogInput();
        out.ddcPort = edidPort;
        if (edid.detailedCount() > 0) {
            const DisplayTiming &t = edid.detailedTiming(0);
            out.preferredWidth = (uint16_t)t.hDisplay;
            out.preferredHeight = (uint16_t)t.vDisplay;
            out.preferredHz = (uint16_t)t.refreshHz();
            out.preferredKhz = t.pixelClockKhz;
        }
    }

    if (modeBytes != 0) {
        out.width = (uint16_t)width;
        out.height = (uint16_t)height;
        out.bytesPerPixel = (uint8_t)modeBytes;
        out.pitch = pitch;
        out.pixelKhz = modePll.khz;
    }
    out.vpll = read32(PRAMDAC_VPLL);
    describeFamily(out);

    Format::print(out.primitives, sizeof(out.primitives), "%s", Svga::primitivesName());
    out.accelerationSelected = Svga::accelerated();
    engine->describe(out);
}

void
NvidiaPreTesla::ddcDrive(uint8_t drive, uint8_t bit, bool high) {
    uint8_t value = crtcRead(drive);
    value = high ? (uint8_t)(value | bit) : (uint8_t)(value & ~bit);
    crtcWrite(drive, (uint8_t)(value | 0x01));
}

bool
NvidiaPreTesla::ddcSense(uint8_t sense, uint8_t bit) const {
    return (crtcRead(sense) & bit) != 0;
}

// Releases SCL and waits for it to go high: a slave may hold it low (clock stretching).
bool
NvidiaPreTesla::ddcClockHigh(uint8_t drive, uint8_t sense) {
    ddcDrive(drive, 0x20, true);
    for (unsigned i = 0; i < 1000; i++) {
        if (ddcSense(sense, 0x04)) {
            waitMicroseconds(5);
            return true;
        }
        waitMicroseconds(1);
    }
    return false;
}

void
NvidiaPreTesla::ddcStart(uint8_t drive, uint8_t sense) {
    ddcDrive(drive, 0x10, true);
    ddcClockHigh(drive, sense);
    ddcDrive(drive, 0x10, false);
    waitMicroseconds(5);
    ddcDrive(drive, 0x20, false);
    waitMicroseconds(5);
}

void
NvidiaPreTesla::ddcStop(uint8_t drive, uint8_t sense) {
    ddcDrive(drive, 0x10, false);
    waitMicroseconds(5);
    ddcClockHigh(drive, sense);
    ddcDrive(drive, 0x10, true);
    waitMicroseconds(5);
}

bool
NvidiaPreTesla::ddcWrite(uint8_t drive, uint8_t sense, uint8_t byte) {
    for (int bit = 7; bit >= 0; bit--) {
        ddcDrive(drive, 0x10, (byte >> bit & 1) != 0);
        waitMicroseconds(2);
        if (!ddcClockHigh(drive, sense)) {
            return false;
        }
        ddcDrive(drive, 0x20, false);
        waitMicroseconds(2);
    }
    ddcDrive(drive, 0x10, true);
    waitMicroseconds(2);
    if (!ddcClockHigh(drive, sense)) {
        return false;
    }
    bool acknowledged = !ddcSense(sense, 0x08);
    ddcDrive(drive, 0x20, false);
    waitMicroseconds(2);
    return acknowledged;
}

uint8_t
NvidiaPreTesla::ddcRead(uint8_t drive, uint8_t sense, bool acknowledge) {
    uint8_t byte = 0;
    ddcDrive(drive, 0x10, true);
    for (int bit = 7; bit >= 0; bit--) {
        waitMicroseconds(2);
        ddcClockHigh(drive, sense);
        if (ddcSense(sense, 0x08)) {
            byte = (uint8_t)(byte | 1 << bit);
        }
        ddcDrive(drive, 0x20, false);
    }
    ddcDrive(drive, 0x10, !acknowledge);
    waitMicroseconds(2);
    ddcClockHigh(drive, sense);
    ddcDrive(drive, 0x20, false);
    ddcDrive(drive, 0x10, true);
    waitMicroseconds(2);
    return byte;
}

bool
NvidiaPreTesla::readEdid(uint8_t drive, uint8_t sense, uint8_t *block) {
    uint8_t saved = crtcRead(drive);
    ddcDrive(drive, 0x30, true);
    waitMicroseconds(10);
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
