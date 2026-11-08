#include "drivers/video/nvidiaPreTesla2d/NvidiaPreTesla2d.h"
#include "drivers/video/nvidiaPreTesla/NvidiaPreTesla.h"
#include "kernel/Clock.h"
#include "kernel/KernelLog.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Ports.h"

NvidiaPreTesla2d *NvidiaPreTesla2d::running = nullptr;

const SvgaPrimitives NvidiaPreTesla2d::PRIMITIVES = {
    "NVIDIA 2D engine", &NvidiaPreTesla2d::enginePlot, &NvidiaPreTesla2d::engineSpan, &NvidiaPreTesla2d::engineFill,
    &NvidiaPreTesla2d::engineCopy, &NvidiaPreTesla2d::engineSync,
};

// A register (index of a 32-bit word) of an engine and its value, for the tables nvidiafb loads.
class Nv2dWrite {
public:
    uint16_t index;
    uint32_t value;
};

// RAMIN of the NV04 family: the hash table (handles 0x80000010 to 0x80000017 for the eight subchannels) and the
// objects: a DMA object over the frame buffer, the surfaces (class 0x42), ROP (0x43), pattern (0x44), clip (0x19),
// line (0x5C), image blit (0x5F), GDI rectangle (0x4A), stretched blit (0x77) and the push buffer's DMA object.
// 0x0801 (the frame buffer limit) and 0x0826 (the push buffer address) depend on the display.
static const Nv2dWrite NV04_RAMIN[] = {
    {0x0000, 0x80000010}, {0x0001, 0x80011201}, {0x0002, 0x80000011}, {0x0003, 0x80011202},
    {0x0004, 0x80000012}, {0x0005, 0x80011203}, {0x0006, 0x80000013}, {0x0007, 0x80011204},
    {0x0008, 0x80000014}, {0x0009, 0x80011205}, {0x000A, 0x80000015}, {0x000B, 0x80011206},
    {0x000C, 0x80000016}, {0x000D, 0x80011207}, {0x000E, 0x80000017}, {0x000F, 0x80011208},
    {0x0800, 0x00003000}, {0x0801, 0x00000000}, {0x0802, 0x00000002}, {0x0803, 0x00000002},
    {0x0804, 0x01008042}, {0x0805, 0x00000000}, {0x0806, 0x12001200}, {0x0807, 0x00000000},
    {0x0808, 0x01008043}, {0x0809, 0x00000000}, {0x080A, 0x00000000}, {0x080B, 0x00000000},
    {0x080C, 0x01008044}, {0x080D, 0x00000002}, {0x080E, 0x00000000}, {0x080F, 0x00000000},
    {0x0810, 0x01008019}, {0x0811, 0x00000000}, {0x0812, 0x00000000}, {0x0813, 0x00000000},
    {0x0814, 0x0100A05C}, {0x0815, 0x00000000}, {0x0816, 0x00000000}, {0x0817, 0x00000000},
    {0x0818, 0x0100805F}, {0x0819, 0x00000000}, {0x081A, 0x12001200}, {0x081B, 0x00000000},
    {0x081C, 0x0100804A}, {0x081D, 0x00000002}, {0x081E, 0x00000000}, {0x081F, 0x00000000},
    {0x0820, 0x01018077}, {0x0821, 0x00000000}, {0x0822, 0x12001200}, {0x0823, 0x00000000},
    {0x0824, 0x00003002}, {0x0825, 0x00007FFF}, {0x0826, 0x00000000}, {0x0827, 0x00000002},
};

// RAMIN of the NV40 family, the same objects in its layout: surfaces 0x62, ROP 0x43, pattern 0x44, clip 0x19, line
// 0x5C, image blit 0x9F, GDI rectangle 0x4A, stretched blit 0x77. 0x0801 and 0x084E depend on the display.
static const Nv2dWrite NV40_RAMIN[] = {
    {0x0000, 0x80000010}, {0x0001, 0x00101202}, {0x0002, 0x80000011}, {0x0003, 0x00101204},
    {0x0004, 0x80000012}, {0x0005, 0x00101206}, {0x0006, 0x80000013}, {0x0007, 0x00101208},
    {0x0008, 0x80000014}, {0x0009, 0x0010120A}, {0x000A, 0x80000015}, {0x000B, 0x0010120C},
    {0x000C, 0x80000016}, {0x000D, 0x0010120E}, {0x000E, 0x80000017}, {0x000F, 0x00101210},
    {0x0800, 0x00003000}, {0x0801, 0x00000000}, {0x0802, 0x00000002},
    {0x0808, 0x02080062}, {0x0809, 0x00000000}, {0x080A, 0x00001200}, {0x080B, 0x00001200},
    {0x080C, 0x00000000}, {0x080D, 0x00000000},
    {0x0810, 0x02080043}, {0x0811, 0x00000000}, {0x0812, 0x00000000}, {0x0813, 0x00000000},
    {0x0814, 0x00000000}, {0x0815, 0x00000000},
    {0x0818, 0x02080044}, {0x0819, 0x02000000}, {0x081A, 0x00000000}, {0x081B, 0x00000000},
    {0x081C, 0x00000000}, {0x081D, 0x00000000},
    {0x0820, 0x02080019}, {0x0821, 0x00000000}, {0x0822, 0x00000000}, {0x0823, 0x00000000},
    {0x0824, 0x00000000}, {0x0825, 0x00000000},
    {0x0828, 0x020A005C}, {0x0829, 0x00000000}, {0x082A, 0x00000000}, {0x082B, 0x00000000},
    {0x082C, 0x00000000}, {0x082D, 0x00000000},
    {0x0830, 0x0208009F}, {0x0831, 0x00000000}, {0x0832, 0x00001200}, {0x0833, 0x00001200},
    {0x0834, 0x00000000}, {0x0835, 0x00000000},
    {0x0838, 0x0208004A}, {0x0839, 0x02000000}, {0x083A, 0x00000000}, {0x083B, 0x00000000},
    {0x083C, 0x00000000}, {0x083D, 0x00000000},
    {0x0840, 0x02080077}, {0x0841, 0x00000000}, {0x0842, 0x00001200}, {0x0843, 0x00001200},
    {0x0844, 0x00000000}, {0x0845, 0x00000000},
    {0x084C, 0x00003002}, {0x084D, 0x00007FFF}, {0x084E, 0x00000000},
};

// PGRAPH of the NV04 family.
static const Nv2dWrite NV04_GRAPH[] = {
    {0x0080 / 4, 0x000001FF}, {0x0080 / 4, 0x1230C000}, {0x0084 / 4, 0x72111101}, {0x0088 / 4, 0x11D5F071},
    {0x008C / 4, 0x0004FF31}, {0x008C / 4, 0x4004FF31}, {0x0140 / 4, 0x00000000}, {0x0100 / 4, 0xFFFFFFFF},
    {0x0170 / 4, 0x10010100}, {0x0710 / 4, 0xFFFFFFFF}, {0x0720 / 4, 0x00000001}, {0x0810 / 4, 0x00000000},
    {0x0608 / 4, 0xFFFFFFFF},
};

// The test: a 256 x 128 pixel surface at 32 bits per pixel, the top half a pattern.
static constexpr unsigned NV2D_TEST_WIDTH = 256;
static constexpr unsigned NV2D_TEST_HEIGHT = 128;
static constexpr uint32_t NV2D_TEST_PITCH = NV2D_TEST_WIDTH * 4;
static constexpr uint32_t NV2D_TEST_BELOW_PUSH_BUFFER = 4u << 20;
static constexpr uint32_t NV2D_TEST_COLOR = 0x00A5C3E7;

static uint32_t
nv2dPattern(unsigned x, unsigned y) {
    return ((x * 0x00010307u) ^ (y * 0x00A01009u) ^ 0x00336699u) & 0xFFFFFFu;
}

// Millions of pixels a second, times 10.
static uint32_t
nv2dRate(uint64_t pixels, uint64_t nanoseconds) {
    return nanoseconds ? (uint32_t)(pixels * 10000 / nanoseconds) : 0;
}

NvidiaPreTesla2d::NvidiaPreTesla2d(NvidiaPreTesla &owner)
    : display(owner), pushBuffer(nullptr), pushBufferOffset(0), put(0), current(0), freeWords(0), currentRop(~0u),
      started(false), lockedUp(false), busy(false), surfaceReady(false), fills(0), copies(0), kickoffs(0),
      lockups(0) {}

uint32_t
NvidiaPreTesla2d::read32(uint32_t reg) const {
    return *(volatile uint32_t *)(display.card.mmio + reg);
}

void
NvidiaPreTesla2d::write32(uint32_t reg, uint32_t value) {
    *(volatile uint32_t *)(display.card.mmio + reg) = value;
}

// nvidiafb's NVLoadStateExt up to the display registers, which NvidiaPreTesla owns: engines reset, tiling off, the
// objects in RAMIN, PGRAPH set up for the chip, PFIFO's cache 1 on channel 0 in DMA mode with the push buffer.
void
NvidiaPreTesla2d::loadEngine() {
    bool nv40 = display.card.chipset >= 0x40;
    uint32_t implementation = display.card.implementation & 0xFFF0;
    uint32_t map = display.mapBytes();

    write32(PMC + 0x0140, 0x00000000);
    write32(PMC + 0x0200, 0xFFFF00FF);
    write32(PMC + 0x0200, 0xFFFFFFFF);
    write32(PTIMER + 0x0200 * 4, 0x00000008);
    write32(PTIMER + 0x0210 * 4, 0x00000003);
    write32(PTIMER + 0x0140 * 4, 0x00000000);
    write32(PTIMER + 0x0100 * 4, 0xFFFFFFFF);

    if (!nv40) {
        // nouveau's NV04 PFB set-up does the same: the value the blob writes.
        write32(PFB + 0x0200, 0x00001114);
    } else if (implementation == 0x0040) {
        for (unsigned i = 0; i < 8; i++) {
            write32(PFB + 0x0240 + i * 0x10, 0);
            write32(PFB + 0x0244 + i * 0x10, map - 1);
        }
    } else {
        unsigned regions = implementation == 0x0090 || implementation == 0x01D0 || implementation == 0x0290 ||
                           implementation == 0x0390 || implementation == 0x03D0 ? 15 : 12;
        for (unsigned i = 0; i < regions; i++) {
            write32(PFB + 0x0600 + i * 0x10, 0);
            write32(PFB + 0x0604 + i * 0x10, map - 1);
        }
    }

    if (nv40) {
        for (const Nv2dWrite &w : NV40_RAMIN) {
            write32(PRAMIN + w.index * 4u, w.value);
        }
        write32(PRAMIN + 0x0801 * 4, map - 1);
        write32(PRAMIN + 0x084E * 4, pushBufferOffset | 0x00000002);
        loadNv40Graph();
    } else {
        for (const Nv2dWrite &w : NV04_RAMIN) {
            write32(PRAMIN + w.index * 4u, w.value);
        }
        write32(PRAMIN + 0x0801 * 4, map - 1);
        write32(PRAMIN + 0x0826 * 4, pushBufferOffset | 0x00000002);
        if ((display.card.device & 0x0FFF) == 0x0020) {
            // The first TNT reads its push buffer through the PCI address of the frame buffer.
            write32(PRAMIN + 0x0824 * 4, read32(PRAMIN + 0x0824 * 4) | 0x00020000);
            write32(PRAMIN + 0x0826 * 4, read32(PRAMIN + 0x0826 * 4) + (uint32_t)display.card.frameAddress);
            uint16_t command = Pci::read16(display.card.at, Pci::COMMAND);
            Pci::write16(display.card.at, Pci::COMMAND, command | Pci::COMMAND_BUS_MASTER);
        }
        loadNv04Graph();
    }
    write32(PGRAPH + 0x053C, 0);
    write32(PGRAPH + 0x0540, 0);
    write32(PGRAPH + 0x0544, 0x00007FFF);
    write32(PGRAPH + 0x0548, 0x00007FFF);
    loadFifo();
}

void
NvidiaPreTesla2d::loadNv04Graph() {
    for (const Nv2dWrite &w : NV04_GRAPH) {
        write32(PGRAPH + w.index * 4u, w.value);
    }
}

// nvidiafb for NV_ARCH_40. Its writes of PRAMDAC 0x0608 (output configuration of the head) for some chips are left
// out: the display is NvidiaPreTesla's, and works without them.
void
NvidiaPreTesla2d::loadNv40Graph() {
    uint32_t implementation = display.card.implementation & 0xFFF0;
    uint32_t map = display.mapBytes();
    write32(PGRAPH + 0x0080, 0xFFFFFFFF);
    write32(PGRAPH + 0x0080, 0x00000000);
    write32(PGRAPH + 0x0140, 0x00000000);
    write32(PGRAPH + 0x0100, 0xFFFFFFFF);
    write32(PGRAPH + 0x0144, 0x10010100);
    write32(PGRAPH + 0x0714, 0xFFFFFFFF);
    write32(PGRAPH + 0x0720, 0x00000001);
    write32(PGRAPH + 0x0710, read32(PGRAPH + 0x0710) & 0x0007FF00);
    write32(PGRAPH + 0x0710, read32(PGRAPH + 0x0710) | 0x00020100);

    write32(PGRAPH + 0x0084, 0x401287C0);
    write32(PGRAPH + 0x008C, 0x60DE8051);
    write32(PGRAPH + 0x0090, 0x00008000);
    write32(PGRAPH + 0x0610, 0x00BE3C5F);
    write32(PGRAPH + 0x0BC4, read32(PGRAPH + 0x0BC4) | 0x00008000);
    uint32_t units = read32(0x001540) & 0xFF;
    if (units) {
        unsigned first = 0;
        while (!(units & 1)) {
            units >>= 1;
            first++;
        }
        write32(PGRAPH + 0x5000, first);
    }
    if (implementation == 0x0040) {
        write32(PGRAPH + 0x09B0, 0x83280FFF);
        write32(PGRAPH + 0x09B4, 0x000000A0);
    } else {
        write32(PGRAPH + 0x0820, 0x83280EFF);
        write32(PGRAPH + 0x0824, 0x000000A0);
    }
    switch (implementation) {
    case 0x0040:
    case 0x0210:
        write32(PGRAPH + 0x09B8, 0x0078E366);
        write32(PGRAPH + 0x09BC, 0x0000014C);
        write32(PFB + 0x033C, read32(PFB + 0x033C) & 0xFFFF7FFF);
        break;
    case 0x00C0:
    case 0x0120:
        write32(PGRAPH + 0x0828, 0x007596FF);
        write32(PGRAPH + 0x082C, 0x00000108);
        break;
    case 0x0160:
    case 0x01D0:
    case 0x0240:
    case 0x03D0:
        write32(PMC + 0x1700, read32(PFB + 0x020C));
        write32(PMC + 0x1704, 0);
        write32(PMC + 0x1708, 0);
        write32(PMC + 0x170C, read32(PFB + 0x020C));
        write32(PGRAPH + 0x0860, 0);
        write32(PGRAPH + 0x0864, 0);
        break;
    case 0x0140:
        write32(PGRAPH + 0x0828, 0x0072CB77);
        write32(PGRAPH + 0x082C, 0x00000108);
        break;
    case 0x0220:
        write32(PGRAPH + 0x0860, 0);
        write32(PGRAPH + 0x0864, 0);
        break;
    case 0x0090:
    case 0x0290:
    case 0x0390:
        write32(PGRAPH + 0x0828, 0x07830610);
        write32(PGRAPH + 0x082C, 0x0000016A);
        break;
    default:
        break;
    }
    write32(PGRAPH + 0x0B38, 0x2FFFF800);
    write32(PGRAPH + 0x0B3C, 0x00006000);
    write32(PGRAPH + 0x032C, 0x01000000);
    write32(PGRAPH + 0x0220, 0x00001200);

    // The tiling regions of PFB, copied into PGRAPH.
    bool wide = implementation == 0x0090 || implementation == 0x01D0 || implementation == 0x0290 ||
                implementation == 0x0390;
    if (implementation == 0x0040) {
        for (unsigned i = 0; i < 32; i++) {
            write32(PGRAPH + 0x0900 + i * 4, read32(PFB + 0x0240 + i * 4));
            write32(PGRAPH + 0x6900 + i * 4, read32(PFB + 0x0240 + i * 4));
        }
    } else if (wide || implementation == 0x03D0) {
        for (unsigned i = 0; i < 60; i++) {
            write32(PGRAPH + 0x0D00 + i * 4, read32(PFB + 0x0600 + i * 4));
            write32(PGRAPH + 0x6900 + i * 4, read32(PFB + 0x0600 + i * 4));
        }
    } else {
        for (unsigned i = 0; i < 48; i++) {
            write32(PGRAPH + 0x0900 + i * 4, read32(PFB + 0x0600 + i * 4));
            if (implementation != 0x0160 && implementation != 0x0220 && implementation != 0x0240) {
                write32(PGRAPH + 0x6900 + i * 4, read32(PFB + 0x0600 + i * 4));
            }
        }
    }

    // The memory configuration and the frame buffer's range.
    if (implementation == 0x0040) {
        write32(PGRAPH + 0x09A4, read32(PFB + 0x0200));
        write32(PGRAPH + 0x09A8, read32(PFB + 0x0204));
        write32(PGRAPH + 0x69A4, read32(PFB + 0x0200));
        write32(PGRAPH + 0x69A8, read32(PFB + 0x0204));
        write32(PGRAPH + 0x0820, 0);
        write32(PGRAPH + 0x0824, 0);
        write32(PGRAPH + 0x0864, map - 1);
        write32(PGRAPH + 0x0868, map - 1);
    } else {
        if (wide) {
            write32(PGRAPH + 0x0DF0, read32(PFB + 0x0200));
            write32(PGRAPH + 0x0DF4, read32(PFB + 0x0204));
        } else {
            write32(PGRAPH + 0x09F0, read32(PFB + 0x0200));
            write32(PGRAPH + 0x09F4, read32(PFB + 0x0204));
        }
        write32(PGRAPH + 0x69F0, read32(PFB + 0x0200));
        write32(PGRAPH + 0x69F4, read32(PFB + 0x0204));
        write32(PGRAPH + 0x0840, 0);
        write32(PGRAPH + 0x0844, 0);
        write32(PGRAPH + 0x08A0, map - 1);
        write32(PGRAPH + 0x08A4, map - 1);
    }
    write32(PGRAPH + 0x0B20, 0x00000000);
    write32(PGRAPH + 0x0B04, 0xFFFFFFFF);
}

// Cache 1 of PFIFO on channel 0, pulling from the push buffer's DMA object.
void
NvidiaPreTesla2d::loadFifo() {
    bool nv40 = display.card.chipset >= 0x40;
    const Nv2dWrite FIFO[] = {
        {0x0140, 0x00000000}, {0x0141, 0x00000001}, {0x0480, 0x00000000}, {0x0494, 0x00000000},
        {0x0481, nv40 ? 0x00010000u : 0x00000100u}, {0x0490, 0x00000000}, {0x0491, 0x00000000},
        {0x048B, nv40 ? 0x00001213u : 0x00001209u}, {0x0400, 0x00000000}, {0x0414, 0x00000000},
        {0x0084, 0x03000100}, {0x0085, 0x00000110}, {0x0086, 0x00000112}, {0x0143, 0x0000FFFF},
        {0x0496, 0x0000FFFF}, {0x0050, 0x00000000}, {0x0040, 0xFFFFFFFF}, {0x0415, 0x00000001},
        {0x048C, 0x00000000}, {0x04A0, 0x00000000}, {0x0489, 0x000F0078}, {0x0488, 0x00000001},
        {0x0480, 0x00000001}, {0x0494, 0x00000001}, {0x0495, 0x00000001}, {0x0140, 0x00000001},
    };
    for (const Nv2dWrite &w : FIFO) {
        write32(PFIFO + w.index * 4u, w.value);
    }
}

bool
NvidiaPreTesla2d::start() {
    running = nullptr;
    pushBufferOffset = display.usableBytes();
    pushBuffer = (volatile uint32_t *)(display.card.frame + pushBufferOffset);
    loadEngine();
    for (uint32_t i = 0; i < SKIPS; i++) {
        pushBuffer[i] = 0;
    }
    // Subchannel n holds the object of handle 0x80000010 + n.
    for (uint32_t subchannel = 0; subchannel < 8; subchannel++) {
        pushBuffer[SKIPS + 2 * subchannel] = 0x00040000 | subchannel << 13;
        pushBuffer[SKIPS + 2 * subchannel + 1] = 0x80000010 + subchannel;
    }
    put = 0;
    current = 16 + SKIPS;
    freeWords = DMA_MAX - current;
    currentRop = ~0u;
    started = true;
    lockedUp = false;
    busy = false;
    surfaceReady = false;
    kickoff();
    if (!idle()) {
        started = false;
        klogf("Video: NVIDIA 2D: the engine does not run its first commands\n");
        return false;
    }
    running = this;
    klogf("Video: NVIDIA 2D: engine started, push buffer at %u KB of video memory\n", pushBufferOffset >> 10);
    return true;
}

void
NvidiaPreTesla2d::lockup(const char *what) {
    lockedUp = true;
    busy = false;
    lockups++;
    klogf("Video: NVIDIA 2D: the engine stopped answering (%s; GET %u PUT %u, PGRAPH status %08x): drawing with the "
          "CPU\n", what, readGet(), put, read32(PGRAPH_STATUS));
}

bool
NvidiaPreTesla2d::usable() const {
    return started && !lockedUp && surfaceReady && display.modeBytes != 0;
}

// The CPU's writes reach video memory (write combining) before the engine is told to read them; nvidiafb also reads
// the frame buffer and, on i386, writes an I/O port.
void
NvidiaPreTesla2d::writePut(uint32_t value) {
    __atomic_thread_fence(__ATOMIC_SEQ_CST);
    Ports::outb(0x80, 0);
    (void)display.card.frame[0];
    write32(USER_DMA_PUT, value << 2);
    __atomic_thread_fence(__ATOMIC_SEQ_CST);
}

uint32_t
NvidiaPreTesla2d::readGet() const {
    return read32(USER_DMA_GET) >> 2;
}

void
NvidiaPreTesla2d::kickoff() {
    if (lockedUp || current == put) {
        return;
    }
    put = current;
    writePut(put);
    kickoffs++;
    busy = true;
}

// nvidiafb's NVDmaWait: room for size words, wrapping to the start of the push buffer with a jump once the engine
// has read past it.
void
NvidiaPreTesla2d::dmaWait(uint32_t size) {
    size++;
    uint64_t deadline = Clock::nanoseconds() + TIMEOUT_NS;
    while (freeWords < size && !lockedUp) {
        uint32_t get = readGet();
        if (put >= get) {
            freeWords = DMA_MAX - current;
            if (freeWords < size) {
                dmaNext(0x20000000);
                if (get <= SKIPS) {
                    if (put <= SKIPS) {
                        writePut(SKIPS + 1);
                    }
                    do {
                        get = readGet();
                        if (Clock::nanoseconds() > deadline) {
                            lockup("waiting for the push buffer to wrap");
                            return;
                        }
                    } while (get <= SKIPS);
                }
                writePut(SKIPS);
                current = SKIPS;
                put = SKIPS;
                freeWords = get - (SKIPS + 1);
            }
        } else {
            freeWords = get - current - 1;
        }
        if (freeWords < size && Clock::nanoseconds() > deadline) {
            lockup("waiting for room in the push buffer");
        }
    }
}

bool
NvidiaPreTesla2d::dmaStart(uint32_t method, uint32_t size) {
    if (freeWords <= size) {
        dmaWait(size);
    }
    if (lockedUp) {
        return false;
    }
    dmaNext(size << 18 | method);
    freeWords -= size + 1;
    return true;
}

bool
NvidiaPreTesla2d::idle() {
    if (!started || lockedUp) {
        return false;
    }
    if (!busy) {
        return true;
    }
    uint64_t deadline = Clock::nanoseconds() + TIMEOUT_NS;
    while (readGet() != put) {
        if (Clock::nanoseconds() > deadline) {
            lockup("flushing the push buffer");
            return false;
        }
    }
    while (read32(PGRAPH_STATUS) != 0) {
        if (Clock::nanoseconds() > deadline) {
            lockup("waiting for PGRAPH");
            return false;
        }
    }
    busy = false;
    return true;
}

// nvidiafb's NVSetRopSolid for a plain copy, with no plane mask: a solid pattern and ROP 0xCC (source).
void
NvidiaPreTesla2d::setRopCopy() {
    if (currentRop == 0) {
        return;
    }
    if (currentRop >= 16) {
        if (!dmaStart(PATTERN_COLOR_0, 4)) {
            return;
        }
        dmaNext(~0u);
        dmaNext(~0u);
        dmaNext(~0u);
        dmaNext(~0u);
    }
    if (!dmaStart(ROP_SET, 1)) {
        return;
    }
    dmaNext(0xCC);
    currentRop = 0;
}

// The destination (and source) surface, the colour formats of the objects and a clip of the whole surface, as
// nvidiafb's NVResetGraphics.
void
NvidiaPreTesla2d::surface(unsigned bytes, uint32_t surfacePitch, uint32_t offset, unsigned clipWidth,
                          unsigned clipHeight) {
    uint32_t surfaceFormat = bytes == 4 ? 6 : bytes == 2 ? 4 : 1;
    uint32_t objectFormat = bytes == 2 ? 1 : 3;
    if (!dmaStart(SURFACE_FORMAT, 4)) {
        return;
    }
    dmaNext(surfaceFormat);
    dmaNext(surfacePitch | surfacePitch << 16);
    dmaNext(offset);
    dmaNext(offset);
    if (!dmaStart(PATTERN_FORMAT, 1)) {
        return;
    }
    dmaNext(objectFormat);
    if (!dmaStart(RECT_FORMAT, 1)) {
        return;
    }
    dmaNext(objectFormat);
    if (!dmaStart(LINE_FORMAT, 1)) {
        return;
    }
    dmaNext(objectFormat);
    currentRop = ~0u;
    setRopCopy();
    if (!dmaStart(CLIP_POINT, 2)) {
        return;
    }
    dmaNext(0);
    dmaNext((uint32_t)(clipHeight + 1) << 16 | (clipWidth + 1));
    kickoff();
}

void
NvidiaPreTesla2d::setSurface() {
    if (!started || lockedUp || display.modeBytes == 0) {
        surfaceReady = false;
        return;
    }
    surface(display.scanBytes, display.pitch, 0, display.width, display.height);
    surfaceReady = !lockedUp;
}

void
NvidiaPreTesla2d::clear() {
    if (usable()) {
        fill(0, 0, display.width, display.height, 0);
    }
}

void
NvidiaPreTesla2d::fill(unsigned x, unsigned y, unsigned width, unsigned height, uint32_t color) {
    if (!dmaStart(RECT_SOLID_COLOR, 1)) {
        return;
    }
    dmaNext(color);
    if (!dmaStart(RECT_SOLID_RECTS, 2)) {
        return;
    }
    dmaNext((uint32_t)x << 16 | y);
    dmaNext((uint32_t)width << 16 | height);
    kickoff();
    fills++;
}

void
NvidiaPreTesla2d::copy(unsigned fromX, unsigned fromY, unsigned toX, unsigned toY, unsigned width, unsigned height) {
    if (!dmaStart(BLIT_POINT_SOURCE, 3)) {
        return;
    }
    dmaNext((uint32_t)fromY << 16 | fromX);
    dmaNext((uint32_t)toY << 16 | toX);
    dmaNext((uint32_t)height << 16 | width);
    kickoff();
    copies++;
}

// What the CPU writes waits for the engine: it may still be drawing where the CPU writes.
void
NvidiaPreTesla2d::enginePlot(unsigned x, unsigned y, unsigned bytesPerPixel, uint32_t color) {
    NvidiaPreTesla2d *engine = running;
    if (!engine) {
        return;
    }
    if (engine->busy) {
        engine->idle();
    }
    engine->display.writePixel(x, y, bytesPerPixel, color);
}

void
NvidiaPreTesla2d::engineSpan(unsigned x, unsigned y, unsigned bytesPerPixel, const uint8_t *pixels, unsigned count) {
    NvidiaPreTesla2d *engine = running;
    if (!engine) {
        return;
    }
    if (engine->busy) {
        engine->idle();
    }
    engine->display.writeSpan(x, y, bytesPerPixel, pixels, count);
}

void
NvidiaPreTesla2d::engineFill(unsigned x, unsigned y, unsigned width, unsigned height, unsigned bytesPerPixel,
                             uint32_t color) {
    NvidiaPreTesla2d *engine = running;
    if (!engine) {
        return;
    }
    NvidiaPreTesla &display = engine->display;
    if (!engine->usable() || bytesPerPixel != display.modeBytes) {
        if (engine->busy) {
            engine->idle();
        }
        display.fillRect(x, y, width, height, bytesPerPixel, color);
        return;
    }
    if (x >= display.width || y >= display.height) {
        return;
    }
    if (width > display.width - x) {
        width = display.width - x;
    }
    if (height > display.height - y) {
        height = display.height - y;
    }
    engine->fill(x, y, width, height, color & (display.scanBytes == 4 ? 0xFFFFFFu : 0xFFFFFFFFu));
}

bool
NvidiaPreTesla2d::engineCopy(unsigned fromX, unsigned fromY, unsigned toX, unsigned toY, unsigned width,
                             unsigned height) {
    NvidiaPreTesla2d *engine = running;
    if (!engine || !engine->usable()) {
        return false;
    }
    NvidiaPreTesla &display = engine->display;
    if (fromX >= display.width || toX >= display.width || fromY >= display.height || toY >= display.height) {
        return true;
    }
    unsigned right = fromX > toX ? fromX : toX;
    unsigned bottom = fromY > toY ? fromY : toY;
    if (width > display.width - right) {
        width = display.width - right;
    }
    if (height > display.height - bottom) {
        height = display.height - bottom;
    }
    engine->copy(fromX, fromY, toX, toY, width, height);
    return true;
}

void
NvidiaPreTesla2d::engineSync() {
    NvidiaPreTesla2d *engine = running;
    if (engine && engine->busy) {
        engine->idle();
    }
}

void
NvidiaPreTesla2d::describe(NvidiaInfo &out) const {
    out.engineStarted = started;
    out.engineLockedUp = lockedUp;
    out.pushBufferOffset = started ? pushBufferOffset : 0;
    out.pushBufferBytes = PUSH_BUFFER_BYTES;
    out.fills = fills;
    out.copies = copies;
    out.kickoffs = kickoffs;
    out.lockups = lockups;
    out.graphStatus = read32(PGRAPH_STATUS);
    out.fifoGet = started ? readGet() : 0;
    out.fifoPut = put;
}

bool
NvidiaPreTesla2d::selfTest(NvidiaInfo::Test &out) {
    Memory::set(&out, 0, sizeof(out));
    out.ran = true;
    if ((!started || lockedUp) && !start()) {
        Format::print(out.failure, sizeof(out.failure), "the 2D engine does not start");
        return false;
    }
    if (!idle()) {
        Format::print(out.failure, sizeof(out.failure), "the 2D engine does not finish its work");
        return false;
    }
    uint32_t visible = display.modeBytes ? display.pitch * display.height : 0;
    uint32_t usable = display.usableBytes();
    uint32_t testBytes = NV2D_TEST_PITCH * NV2D_TEST_HEIGHT;
    if (usable < NV2D_TEST_BELOW_PUSH_BUFFER + testBytes) {
        Format::print(out.failure, sizeof(out.failure), "too little video memory");
        return false;
    }
    uint32_t area = (usable - NV2D_TEST_BELOW_PUSH_BUFFER) & ~0xFFFFu;
    if (area < visible) {
        Format::print(out.failure, sizeof(out.failure), "no video memory beyond the screen");
        return false;
    }
    volatile uint32_t *pixels = (volatile uint32_t *)(display.card.frame + area);
    const unsigned half = NV2D_TEST_HEIGHT / 2;
    for (unsigned y = 0; y < NV2D_TEST_HEIGHT; y++) {
        for (unsigned x = 0; x < NV2D_TEST_WIDTH; x++) {
            pixels[y * NV2D_TEST_WIDTH + x] = y < half ? nv2dPattern(x, y) : 0;
        }
    }

    // A fill and a copy into the bottom half, then the top half scrolled up a row (an overlapping copy).
    surface(4, NV2D_TEST_PITCH, area, NV2D_TEST_WIDTH, NV2D_TEST_HEIGHT);
    fill(16, 72, 64, 16, NV2D_TEST_COLOR);
    copy(0, 0, 128, 64, 100, 50);
    copy(0, 1, 0, 0, NV2D_TEST_WIDTH, half - 1);
    if (!idle()) {
        Format::print(out.failure, sizeof(out.failure), "the 2D engine stopped during the test");
        return false;
    }
    for (unsigned y = 0; y < NV2D_TEST_HEIGHT; y++) {
        for (unsigned x = 0; x < NV2D_TEST_WIDTH; x++) {
            uint32_t expected = 0;
            if (y + 1 < half) {
                expected = nv2dPattern(x, y + 1);
            } else if (y + 1 == half) {
                expected = nv2dPattern(x, y);
            } else if (x >= 128 && x < 228 && y >= 64 && y < 114) {
                expected = nv2dPattern(x - 128, y - 64);
            } else if (x >= 16 && x < 80 && y >= 72 && y < 88) {
                expected = NV2D_TEST_COLOR;
            }
            uint32_t found = pixels[y * NV2D_TEST_WIDTH + x] & 0xFFFFFFu;
            out.checkedPixels++;
            if (found != expected) {
                if (out.wrongPixels == 0) {
                    out.firstWrongX = x;
                    out.firstWrongY = y;
                    out.expected = expected;
                    out.found = found;
                }
                out.wrongPixels++;
            }
        }
    }

    // Speeds: the whole surface filled, its top half copied onto the bottom one.
    const unsigned rounds = 64;
    const unsigned cpuRounds = 4;
    const uint64_t surfacePixels = NV2D_TEST_WIDTH * NV2D_TEST_HEIGHT;
    const uint64_t halfPixels = NV2D_TEST_WIDTH * half;
    uint64_t started0 = Clock::nanoseconds();
    for (unsigned i = 0; i < rounds; i++) {
        fill(0, 0, NV2D_TEST_WIDTH, NV2D_TEST_HEIGHT, i * 0x010101u);
    }
    bool ok = idle();
    out.engineFill = nv2dRate(rounds * surfacePixels, Clock::nanoseconds() - started0);
    started0 = Clock::nanoseconds();
    for (unsigned i = 0; ok && i < rounds; i++) {
        copy(0, 0, 0, half, NV2D_TEST_WIDTH, half);
    }
    ok = ok && idle();
    out.engineCopy = nv2dRate(rounds * halfPixels, Clock::nanoseconds() - started0);
    started0 = Clock::nanoseconds();
    for (unsigned i = 0; i < cpuRounds; i++) {
        for (unsigned p = 0; p < surfacePixels; p++) {
            pixels[p] = i * 0x010101u;
        }
    }
    out.cpuFill = nv2dRate(cpuRounds * surfacePixels, Clock::nanoseconds() - started0);
    started0 = Clock::nanoseconds();
    for (unsigned i = 0; i < cpuRounds; i++) {
        for (unsigned p = 0; p < halfPixels; p++) {
            pixels[halfPixels + p] = pixels[p];
        }
    }
    out.cpuCopy = nv2dRate(cpuRounds * halfPixels, Clock::nanoseconds() - started0);

    if (display.modeBytes != 0) {
        setSurface();
    } else {
        surfaceReady = false;
    }
    if (!ok) {
        Format::print(out.failure, sizeof(out.failure), "the 2D engine stopped while timed");
        return false;
    }
    out.passed = out.wrongPixels == 0;
    if (!out.passed) {
        Format::print(out.failure, sizeof(out.failure), "%u of %u pixels differ", out.wrongPixels,
                      out.checkedPixels);
    }
    klogf("Video: NVIDIA 2D: self test %s (%u wrong pixels of %u); fill %u.%u / CPU %u.%u, copy %u.%u / CPU %u.%u "
          "Mpixel/s\n", out.passed ? "passed" : "FAILED", out.wrongPixels, out.checkedPixels, out.engineFill / 10,
          out.engineFill % 10, out.cpuFill / 10, out.cpuFill % 10, out.engineCopy / 10, out.engineCopy % 10,
          out.cpuCopy / 10, out.cpuCopy % 10);
    return out.passed;
}
