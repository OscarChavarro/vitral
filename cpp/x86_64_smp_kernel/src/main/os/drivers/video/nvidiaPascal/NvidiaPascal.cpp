#include "drivers/video/nvidiaPascal/NvidiaPascal.h"
#include "drivers/usb/pl2303/Pl2303.h"
#include "fs/Err.h"
#include "kernel/Clock.h"
#include "kernel/KernelLog.h"
#include "kernel/Paging.h"
#include "kernel/PhysicalMemory.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Memory.h"

volatile uint8_t *NvidiaPascal::mmio = nullptr;
Pci::Location NvidiaPascal::location;
unsigned NvidiaPascal::chip = 0;
uint64_t NvidiaPascal::aperture = 0;
uint64_t NvidiaPascal::apertureSize = 0;
bool NvidiaPascal::apertureChecked = false;
bool NvidiaPascal::apertureLinear = false;
unsigned NvidiaPascal::litHead = 0;
unsigned NvidiaPascal::rasterWidthPixels = 0;
unsigned NvidiaPascal::rasterHeightPixels = 0;
bool NvidiaPascal::started = false;
uint32_t *NvidiaPascal::push = nullptr;
uint32_t NvidiaPascal::pushPut = 0;

static uint16_t
nvPascalRead16(const uint8_t *rom, unsigned offset) {
    return (uint16_t)(rom[offset] | rom[offset + 1] << 8);
}

static uint32_t
nvPascalRead32(const uint8_t *rom, unsigned offset) {
    return (uint32_t)nvPascalRead16(rom, offset) | (uint32_t)nvPascalRead16(rom, offset + 2) << 16;
}

const char *
NvidiaPascal::chipName(unsigned chipset) {
    switch (chipset) {
    case 0x130: return "GP100";
    case 0x132: return "GP102";
    case 0x134: return "GP104";
    case 0x136: return "GP106";
    case 0x137: return "GP107";
    case 0x138: return "GP108";
    default: return "GP10x";
    }
}

bool
NvidiaPascal::describe() {
    Pci::Location at;
    bool first = true;
    while (Pci::findClass(0x03, 0x00, first, at)) {
        first = false;
        if (Pci::read16(at, Pci::VENDOR_ID) != 0x10DE) {
            continue;
        }
        uint64_t registers;
        uint64_t registersSize;
        if (!Pci::memoryBar(at, 0, registers, registersSize) || registersSize < (16u << 20) ||
            registers + registersSize > Paging::mappedBytes()) {
            continue;
        }
        volatile uint8_t *registerBar = (volatile uint8_t *)(uintptr_t)registers;
        uint32_t boot0 = *(volatile uint32_t *)(registerBar + PMC_BOOT_0);
        unsigned chipset = (boot0 >> 20) & 0x1FF;
        if (!pascal(chipset)) {
            continue;
        }
        mmio = registerBar;
        location = at;
        chip = chipset;
        if (!Pci::memoryBar(at, 1, aperture, apertureSize)) {
            aperture = apertureSize = 0;
        }
        kprintf("Video: NVIDIA %s (10de:%04x, PMC_BOOT_0 0x%08x) at PCI %u:%u.%u, registers at 0x%08x, VRAM aperture "
                "at 0x%x%08x (%u MB)\n", chipName(chipset), (uint32_t)Pci::read16(at, Pci::DEVICE_ID), boot0,
                (uint32_t)at.bus, (uint32_t)at.device, (uint32_t)at.function, (uint32_t)registers,
                (uint32_t)(aperture >> 32), (uint32_t)aperture, (uint32_t)(apertureSize >> 20));
        const uint8_t *rom = (const uint8_t *)(uintptr_t)LEGACY_ROM;
        if (rom[0] == 0x55 && rom[1] == 0xAA) {
            unsigned bytes = rom[2] * 512u;
            describeBios(rom, bytes < LEGACY_ROM_BYTES ? bytes : LEGACY_ROM_BYTES);
        } else {
            kprintf("Video: %s: no video BIOS at 0x%x (not the boot card?)\n", chipName(chipset),
                    (uint32_t)LEGACY_ROM);
        }
        describeHeads();
        return true;
    }
    return false;
}

// nouveau's dcb.c: the DCB 4.x table, pointed at from offset 0x36 of the image, then its I2C table.
void
NvidiaPascal::describeBios(const uint8_t *rom, unsigned bytes) {
    static const char *const TYPES[16] = {"VGA", "TV", "TMDS", "LVDS", "?", "?", "DP", "?",
                                          "?", "?", "?", "?", "?", "?", "end", "unused"};
    unsigned dcb = nvPascalRead16(rom, 0x36);
    if (dcb + 8 > bytes || nvPascalRead32(rom, dcb + 6) != 0x4EDCBDCB) {
        kprintf("Video: video BIOS of %u KB without a DCB table\n", bytes / 1024);
        return;
    }
    unsigned version = rom[dcb];
    unsigned header = rom[dcb + 1];
    unsigned count = rom[dcb + 2];
    unsigned size = rom[dcb + 3];
    kprintf("Video: video BIOS of %u KB, DCB %u.%u at 0x%x: %u entries\n", bytes / 1024, version >> 4, version & 15,
            dcb, count);
    for (unsigned i = 0; i < count && size >= 8; i++) {
        unsigned entry = dcb + header + i * size;
        if (entry + 8 > bytes) {
            break;
        }
        uint32_t first = nvPascalRead32(rom, entry);
        uint32_t second = nvPascalRead32(rom, entry + 4);
        unsigned type = first & 15;
        if (type == 14) {
            break;
        }
        if (type == 15) {
            continue;
        }
        unsigned outputs = (first >> 24) & 15;
        unsigned sor = outputs & 1 ? 0 : outputs & 2 ? 1 : outputs & 4 ? 2 : outputs & 8 ? 3 : 0;
        kprintf("Video:   output %u: %s on SOR %u, connector %u, I2C/AUX port %u, heads 0x%x, link %u (%08x %08x)\n",
                i, TYPES[type], sor, (first >> 12) & 15, (first >> 4) & 15, (first >> 8) & 15, (second >> 4) & 3,
                first, second);
    }
}

// gf119_head_state(): the timings the core channel armed for each head; a head with no total is off. The first one
// lit is the one scanout() drives.
void
NvidiaPascal::describeHeads() {
    bool found = false;
    for (unsigned head = 0; head < HEADS; head++) {
        uint32_t base = ARMED_STATE + head * 0x300;
        uint32_t total = rd(base + 0x414);
        uint32_t syncEnd = rd(base + 0x418);
        uint32_t blankEnd = rd(base + 0x41c);
        uint32_t blankStart = rd(base + 0x420);
        // SET_PIXEL_CLOCK_FREQUENCY, in Hz
        uint32_t clock = rd(base + 0x450) / 1000;
        unsigned width = (blankStart & 0xFFFF) - (blankEnd & 0xFFFF);
        unsigned height = (blankStart >> 16) - (blankEnd >> 16);
        if ((total & 0xFFFF) <= 8 || clock == 0) {
            kprintf("Video:   head %u off\n", head);
            continue;
        }
        kprintf("Video:   head %u: %ux%u, total %ux%u, sync end %u/%u, pixel clock %u kHz\n", head, width, height,
                total & 0xFFFF, total >> 16, syncEnd & 0xFFFF, syncEnd >> 16, clock);
        if (!found) {
            found = true;
            litHead = head;
            rasterWidthPixels = width;
            rasterHeightPixels = height;
        }
    }
}

bool
NvidiaPascal::waitFor(uint32_t reg, uint32_t bits, uint32_t value, unsigned milliseconds) {
    uint64_t end = Clock::uptimeMs() + milliseconds;
    for (;;) {
        if ((rd(reg) & bits) == value) {
            return true;
        }
        if (Clock::uptimeMs() > end) {
            return false;
        }
        asm volatile("pause");
    }
}

// nv50_instobj_wr32_slow(): PRAMIN shows 1 MB of VRAM, chosen by 0x001700 in 64 KB units.
void
NvidiaPascal::vramWrite(uint64_t address, uint32_t value) {
    wr(PRAMIN_WINDOW, (uint32_t)((address & ~0xFFFFFull) >> 16));
    wr(PRAMIN + (uint32_t)(address & 0xFFFFF), value);
}

uint32_t
NvidiaPascal::vramRead(uint64_t address) {
    wr(PRAMIN_WINDOW, (uint32_t)((address & ~0xFFFFFull) >> 16));
    return rd(PRAMIN + (uint32_t)(address & 0xFFFFF));
}

void
NvidiaPascal::logState(const char *when) {
    klogf("Video: EVO %s: 0x610010 %08x, 0x6100ac %08x, 0x6194e8 %08x, core control 0x610490 %08x, put %08x, get "
          "%08x, error 0x6111f0 %08x %08x %08x (pending 0x61009c %08x), PMC %08x/%08x\n", when, rd(0x610010),
          rd(0x6100ac), rd(0x6194e8), rd(0x610490), rd(CORE_PUT), rd(CORE_GET), rd(CORE_ERROR), rd(CORE_ERROR + 4),
          rd(CORE_ERROR + 8), rd(0x61009c), rd(0x000100), rd(0x000140));
    settle();
}

// Only while starting: a mode is set with the SVGA lock held, where the USB adapter must not be driven
void
NvidiaPascal::settle() {
    if (!started) {
        Pl2303::flushLog(1000);
    }
}

int
NvidiaPascal::start() {
    if (!mmio || rasterWidthPixels == 0 || rasterHeightPixels == 0) {
        return -Err::NODEV;
    }
    if (started) {
        return 0;
    }
    klogf("Video: EVO on %s: 0x022448 %08x (heads), PMC_ENABLE %08x, head %u lit at %ux%u\n", chipName(chip),
          rd(0x022448), rd(0x000200), litHead, rasterWidthPixels, rasterHeightPixels);
    settle();
    int result = takeDisplay();
    if (result == 0) {
        result = startCoreChannel();
    }
    if (result == 0) {
        bindVramContext();
        started = true;
    }
    return result;
}

// gf119_disp_init(): the capabilities of the heads and the SORs, copied where the engine expects them, the display
// taken from the video BIOS, the instance memory given to the engine (VRAM target) and the interrupts it reports set.
int
NvidiaPascal::takeDisplay() {
    uint32_t heads = rd(0x022448) & 0xF;
    if (heads == 0 || heads > HEADS) {
        heads = HEADS;
    }
    logState("before");
    // The GPU reads the push buffer from system memory; its legacy interrupt line stays off (the steps poll).
    Pci::write16(location, Pci::COMMAND,
                 (uint16_t)(Pci::read16(location, Pci::COMMAND) | Pci::COMMAND_MEMORY | Pci::COMMAND_BUS_MASTER |
                            Pci::COMMAND_INTERRUPT_DISABLE));
    for (uint32_t i = 0; i < heads; i++) {
        uint32_t offset = i * 0x800;
        wr(0x6101b4 + offset, rd(0x616104 + offset));
        wr(0x6101b8 + offset, rd(0x616108 + offset));
        wr(0x6101bc + offset, rd(0x61610c + offset));
    }
    for (uint32_t i = 0; i < SORS; i++) {
        wr(0x6301c4 + i * 0x800, rd(0x61c000 + i * 0x800));
    }
    if (rd(0x6100ac) & 0x100) {
        wr(0x6100ac, 0x100);
        mask(0x6194e8, 0x1, 0);
        if (!waitFor(0x6194e8, 0x2, 0, 2000)) {
            logState("the video BIOS keeps the display");
            return -Err::IO;
        }
    }
    logState("taken from the video BIOS");
    uint32_t oldWindow = rd(PRAMIN_WINDOW);
    for (uint32_t offset = 0; offset < DISPLAY_INSTANCE_BYTES; offset += 4) {
        vramWrite(DISPLAY_INSTANCE_VRAM + offset, 0);
    }
    vramWrite(DISPLAY_INSTANCE_VRAM, 0x12345678);
    uint32_t check = vramRead(DISPLAY_INSTANCE_VRAM);
    vramWrite(DISPLAY_INSTANCE_VRAM, 0);
    wr(PRAMIN_WINDOW, oldWindow);
    klogf("Video: EVO instance memory at VRAM 0x%x (PRAMIN check %s: 0x%08x)\n", (uint32_t)DISPLAY_INSTANCE_VRAM,
          check == 0x12345678 ? "ok" : "failed", check);
    settle();
    if (check != 0x12345678) {
        return -Err::IO;
    }
    wr(0x610010, (uint32_t)(DISPLAY_INSTANCE_VRAM >> 8) | 9);
    // The GPU raises no interrupt towards the CPU (its two interrupt trees off at the source): the display reports its
    // channel errors and supervisor stages, and they are polled in 0x61009c and 0x6100ac. Underflow reporting off.
    wr(0x000140, 0);
    wr(0x000144, 0);
    wr(0x610090, 0);
    wr(0x6100a0, 1);
    wr(0x6100b0, 0x307);
    for (uint32_t i = 0; i < heads; i++) {
        mask(0x616308 + i * 0x800, 0x111, 0x010);
    }
    return 0;
}

// gp102_disp_core_init(): a push buffer page in system memory (target 3, coherent), its address in 256 byte units,
// then one method to check that the engine fetches.
int
NvidiaPascal::startCoreChannel() {
    if (!push) {
        uint64_t page = PhysicalMemory::allocContiguous(1, 1ull << 32);
        if (!page) {
            return -Err::NOMEM;
        }
        push = (uint32_t *)(uintptr_t)page;
    }
    uint64_t page = (uint64_t)(uintptr_t)push;
    pushPut = 0;
    Memory::set(push, 0, PUSH_BYTES);
    uint64_t pushValue = (page >> 8) | 3;
    wr(0x611494, (uint32_t)pushValue);
    wr(0x611498, (uint32_t)(pushValue >> 32));
    wr(0x61149c, 1);
    mask(0x610490, 0, 0x10);
    wr(CORE_PUT, 0);
    wr(0x610490, 0x01000013);
    bool idle = waitFor(0x610490, 0x80000000, 0, 2000);
    logState(idle ? "core channel started" : "core channel does not start");
    if (!idle) {
        return -Err::IO;
    }
    method(SET_NOTIFIER_CONTROL, 0);
    bool fetched = kick(1000);
    logState(fetched ? "method fetched" : "method not fetched");
    return fetched ? 0 : -Err::IO;
}

// The hash table (at the start of the instance memory) gets the VRAM context DMA of gf119_dmaobj_bind() (pitch
// memory from 0 to 1 GB), which the heads' CONTEXT_DMAS_ISO name. Context: object offset << 9 | 1 (channel 0).
void
NvidiaPascal::bindVramContext() {
    uint32_t oldWindow = rd(PRAMIN_WINDOW);
    uint64_t dma = DISPLAY_INSTANCE_VRAM + DMA_OBJECT_OFFSET;
    vramWrite(dma + 0x00, 0x00000009);
    vramWrite(dma + 0x04, 0);
    vramWrite(dma + 0x08, (uint32_t)((0x40000000ull - 1) >> 8));
    vramWrite(dma + 0x0c, 0);
    vramWrite(dma + 0x10, 0);
    vramWrite(dma + 0x14, 0);
    uint32_t hash = 0;
    for (uint32_t handle = VRAM_HANDLE; handle; handle >>= RAMHT_BITS) {
        hash ^= handle & ((1u << RAMHT_BITS) - 1);
    }
    vramWrite(DISPLAY_INSTANCE_VRAM + hash * 8, VRAM_HANDLE);
    vramWrite(DISPLAY_INSTANCE_VRAM + hash * 8 + 4, DMA_OBJECT_OFFSET << 9 | 1);
    wr(PRAMIN_WINDOW, oldWindow);
}

// A word written through BAR1 reads back through PRAMIN: BAR1 maps VRAM linearly.
bool
NvidiaPascal::checkAperture() {
    if (apertureChecked) {
        return apertureLinear;
    }
    apertureChecked = true;
    if (!aperture || aperture + apertureSize > Paging::mappedBytes()) {
        return false;
    }
    static constexpr uint64_t PROBE = 0x01000000;
    volatile uint32_t *word = (volatile uint32_t *)(uintptr_t)(aperture + PROBE);
    uint32_t oldWindow = rd(PRAMIN_WINDOW);
    uint32_t old = *word;
    *word = 0x5A17C0DE;
    asm volatile("mfence" : : : "memory");
    uint32_t seen = vramRead(PROBE);
    *word = old;
    wr(PRAMIN_WINDOW, oldWindow);
    apertureLinear = seen == 0x5A17C0DE;
    klogf("Video: VRAM aperture at 0x%x%08x (%u MB): %s (VRAM 0x%x read 0x%08x through PRAMIN)\n",
          (uint32_t)(aperture >> 32), (uint32_t)aperture, (uint32_t)(apertureSize >> 20),
          apertureLinear ? "linear" : "not linear", (uint32_t)PROBE, seen);
    return apertureLinear;
}

volatile uint8_t *
NvidiaPascal::vram(uint64_t offset, uint64_t bytes) {
    if (!mmio || offset + bytes > apertureSize || !checkAperture()) {
        return nullptr;
    }
    return (volatile uint8_t *)(uintptr_t)(aperture + offset);
}

// Room for the next methods: when they would not fit before the end of the push buffer, a JUMP to its start
// (nv50_dmac_wind()). The channel is idle between batches, so GET is past the jump's position.
void
NvidiaPascal::reserve(unsigned bytes) {
    if (pushPut + bytes + 8 > PUSH_BYTES) {
        push[pushPut / 4] = 0x20000000;
        pushPut = 0;
    }
}

// One method with one data word: header (count << 18) | method
void
NvidiaPascal::method(uint32_t mthd, uint32_t data) {
    push[pushPut / 4] = 1u << 18 | mthd;
    push[pushPut / 4 + 1] = data;
    pushPut += 8;
}

bool
NvidiaPascal::kick(unsigned milliseconds) {
    if (pushPut == rd(CORE_GET)) {
        // PUT equal to GET would be taken for an idle channel (after a jump the batch can end where the last one did)
        method(SET_NOTIFIER_CONTROL, 0);
    }
    asm volatile("mfence" : : : "memory");
    wr(CORE_PUT, pushPut);
    if (waitFor(CORE_GET, 0xFFFFFFFF, pushPut, milliseconds)) {
        // Fetched, but a method may still have raised an exception (the channel goes on after the last one)
        uint32_t pending = rd(0x61009c) & 1;
        if (!pending) {
            return true;
        }
        uint32_t mthd = rd(CORE_ERROR);
        klogf("Video: EVO exception after get %08x: method 0x%04x (0x6111f0 %08x) data %08x error %08x\n",
              rd(CORE_GET), mthd & 0xffc, mthd, rd(CORE_ERROR + 4), rd(CORE_ERROR + 8));
        wr(CORE_ERROR, 0x90000000);
        wr(0x61009c, pending);
        settle();
        return false;
    }
    // gp102_disp_intr_error(): the method, its data and the error type of the core channel, then acknowledged (the
    // channel stays halted on the failing method until then)
    uint32_t pending = rd(0x61009c);
    uint32_t mthd = rd(CORE_ERROR);
    klogf("Video: EVO channel stalled at get %08x: pending 0x61009c %08x, method 0x%04x (0x6111f0 %08x) data %08x "
          "error %08x\n", rd(CORE_GET), pending, mthd & 0xffc, mthd, rd(CORE_ERROR + 4), rd(CORE_ERROR + 8));
    if (pending || (mthd & 0x80000000)) {
        wr(CORE_ERROR, 0x90000000);
        wr(0x61009c, pending);
    }
    settle();
    return waitFor(CORE_GET, 0xFFFFFFFF, pushPut, 200);
}

// The core channel starts with an assembly state of defaults (an 8x5 raster, no pixel clock, no SOR), which UPDATE
// would commit with the surface. The mode methods of the head and the SOR controls are sent again with what the video
// BIOS armed, so that UPDATE keeps that mode. Once an UPDATE has committed them both states agree and nothing is sent.
unsigned
NvidiaPascal::adoptArmedMode(unsigned head) {
    // cl907d.h: CONTROL_OUTPUT_RESOURCE, CONTROL, OVERSCAN_COLOR, RASTER_SIZE, RASTER_SYNC_END, RASTER_BLANK_END,
    // RASTER_BLANK_START, RASTER_VERT_BLANK2, DEFAULT_BASE_COLOR, PIXEL_CLOCK_FREQUENCY, PIXEL_CLOCK_CONFIGURATION,
    // PIXEL_CLOCK_FREQUENCY_MAX, CONTROL_CURSOR, DITHER_CONTROL, PROCAMP, BASE_CHANNEL_USAGE_BOUNDS,
    // OVERLAY_USAGE_BOUNDS
    static const uint32_t HEAD_MODE_METHODS[] = {0x404, 0x408, 0x410, 0x414, 0x418, 0x41c, 0x420, 0x424, 0x42c,
                                                 0x450, 0x454, 0x458, 0x480, 0x490, 0x498, 0x4d0, 0x4d4};
    unsigned sent = 0;
    for (uint32_t sor = 0; sor < SORS; sor++) {
        uint32_t mthd = 0x200 + sor * 0x20;
        uint32_t armed = rd(ARMED_STATE + mthd);
        if (armed != rd(ASSEMBLY_STATE + mthd)) {
            klogf("Video: EVO SOR %u control %08x\n", sor, armed);
            method(mthd, armed);
            sent++;
        }
    }
    for (uint32_t base : HEAD_MODE_METHODS) {
        uint32_t mthd = base + head * 0x300;
        uint32_t armed = rd(ARMED_STATE + mthd);
        if (base == 0x454) {
            // PIXEL_CLOCK_CONFIGURATION: the video BIOS arms it with NOT_DRIVER (bit 24), which a driver may not send
            armed &= ~(1u << 24);
        }
        if (armed != rd(ASSEMBLY_STATE + mthd)) {
            klogf("Video: EVO head %u method 0x%03x = %08x (assembly had %08x)\n", head, mthd, armed,
                  rd(ASSEMBLY_STATE + mthd));
            method(mthd, armed);
            sent++;
        }
    }
    return sent;
}

bool
NvidiaPascal::scanout(uint64_t offset, unsigned width, unsigned height, unsigned pitch) {
    if (!started || width == 0 || height == 0 || width > rasterWidthPixels || height > rasterHeightPixels ||
        (offset & 0xFF) || (pitch & 0xFF) || pitch < width * 4) {
        return false;
    }
    // The viewport out keeps the aspect of the surface inside the raster; the head centres it
    unsigned outWidth = rasterWidthPixels;
    unsigned outHeight = rasterHeightPixels;
    if ((uint64_t)width * rasterHeightPixels >= (uint64_t)height * rasterWidthPixels) {
        outHeight = (unsigned)((uint64_t)height * rasterWidthPixels / width);
    } else {
        outWidth = (unsigned)((uint64_t)width * rasterHeightPixels / height);
    }
    uint32_t hoff = litHead * 0x300;
    reserve(64 * 8);
    unsigned adopted = adoptArmedMode(litHead);
    method(HEAD_SET_CONTROL_OUTPUT_SCALER + hoff, 0);
    method(HEAD_SET_OFFSET + hoff, (uint32_t)(offset >> 8));
    method(HEAD_SET_SIZE + hoff, height << 16 | width);
    method(HEAD_SET_STORAGE + hoff, STORAGE_PITCH_LAYOUT | (pitch >> 8) << 8);
    method(HEAD_SET_PARAMS + hoff, FORMAT_A8R8G8B8 << 8);
    method(HEAD_SET_CONTEXT_DMAS_ISO + hoff, VRAM_HANDLE);
    method(HEAD_SET_VIEWPORT_POINT_IN + hoff, 0);
    method(HEAD_SET_VIEWPORT_SIZE_IN + hoff, height << 16 | width);
    method(HEAD_SET_VIEWPORT_SIZE_OUT + hoff, outHeight << 16 | outWidth);
    method(HEAD_SET_VIEWPORT_SIZE_OUT_MIN + hoff, outHeight << 16 | outWidth);
    method(HEAD_SET_VIEWPORT_SIZE_OUT_MAX + hoff, outHeight << 16 | outWidth);
    method(UPDATE, 0);
    klogf("Video: EVO scanout of %ux%u (pitch %u) at VRAM 0x%x, shown at %ux%u on the %ux%u raster of head %u (%u "
          "methods of the armed mode)\n", width, height, pitch, (uint32_t)offset, outWidth, outHeight,
          rasterWidthPixels, rasterHeightPixels, litHead, adopted);
    settle();
    bool fetched = kick(1000);
    supervise(litHead, offset);
    bool armed = rd(ARMED_STATE + HEAD_SET_OFFSET + hoff) == (uint32_t)(offset >> 8) &&
                 rd(ARMED_STATE + HEAD_SET_VIEWPORT_SIZE_IN + hoff) == (height << 16 | width);
    if (!fetched || !armed) {
        logState("scanout not armed");
    }
    logScanout(litHead);
    return armed;
}

// The supervisor stages after an UPDATE (0x6100ac bits 0 to 2), with what each head changes in 0x6101d4 + head *
// 0x800: acknowledged as gf119_disp_super() does once its work is done, here none (the clocks and outputs keep their
// settings). Ends once the head has armed the surface and no stage came for a while.
void
NvidiaPascal::supervise(unsigned head, uint64_t offset) {
    uint32_t hoff = head * 0x300;
    uint64_t now = Clock::uptimeMs();
    uint64_t end = now + 2000;
    uint64_t quietSince = now;
    while (now < end) {
        uint32_t pending = rd(0x6100ac) & 7;
        if (pending) {
            klogf("Video: EVO supervisor %u: 0x6100ac %08x, head masks %08x %08x %08x %08x\n",
                  pending & 1 ? 1 : pending & 2 ? 2 : 3, rd(0x6100ac), rd(0x6101d4), rd(0x6101d4 + 0x800),
                  rd(0x6101d4 + 0x1000), rd(0x6101d4 + 0x1800));
            wr(0x6100ac, pending);
            for (uint32_t i = 0; i < HEADS; i++) {
                wr(0x6101d4 + i * 0x800, 0);
            }
            wr(0x6101d0, 0x80000000);
            quietSince = Clock::uptimeMs();
        } else if (rd(ARMED_STATE + HEAD_SET_OFFSET + hoff) == (uint32_t)(offset >> 8) && now - quietSince > 50) {
            return;
        }
        asm volatile("pause");
        now = Clock::uptimeMs();
    }
}

// Whether the head scans (gf119_head_rgpos(): its line and pixel counters, read twice) and the power of the SORs
void
NvidiaPascal::logScanout(unsigned head) {
    uint32_t first = rd(0x616340 + head * 0x800);
    uint64_t end = Clock::uptimeMs() + 2;
    while (Clock::uptimeMs() < end) {
        asm volatile("pause");
    }
    uint32_t second = rd(0x616340 + head * 0x800);
    klogf("Video: EVO head %u position %08x then %08x (%s); SOR power %08x %08x %08x %08x; armed offset %08x size "
          "%08x viewport %08x -> %08x\n", head, first, second, first != second ? "scanning" : "stopped",
          rd(0x61c004), rd(0x61c804), rd(0x61d004), rd(0x61d804), rd(ARMED_STATE + 0x460 + head * 0x300),
          rd(ARMED_STATE + 0x468 + head * 0x300), rd(ARMED_STATE + 0x4b8 + head * 0x300),
          rd(ARMED_STATE + 0x4c0 + head * 0x300));
    settle();
}

int
NvidiaPascal::dumpArmedState() {
    if (!mmio) {
        return -Err::NODEV;
    }
    static const struct {
        uint32_t first;
        uint32_t last;
        const char *what;
    } AREAS[] = {
        {0x000, 0x0fc, "core"}, {0x180, 0x1fc, "DAC/PIOR"}, {0x200, 0x27c, "SOR"}, {0x300, 0x3fc, "misc"},
        {0x400, 0x6fc, "head 0"}, {0x700, 0x9fc, "head 1"},
    };
    for (const auto &area : AREAS) {
        char line[200];
        unsigned length = 0;
        for (uint32_t mthd = area.first; mthd <= area.last; mthd += 4) {
            uint32_t armed = rd(ARMED_STATE + mthd);
            uint32_t assembly = rd(ASSEMBLY_STATE + mthd);
            if (armed == 0 && assembly == 0) {
                continue;
            }
            if (length > 140) {
                klogf("Video: ARM %s:%s\n", area.what, line);
                length = 0;
            }
            length += Format::print(line + length, sizeof(line) - length, " %03x=%08x/%08x", mthd, armed, assembly);
        }
        if (length) {
            klogf("Video: ARM %s:%s\n", area.what, line);
        }
    }
    return 0;
}
