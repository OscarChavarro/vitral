#include "drivers/video/bochsVBE/BochsVbe.h"
#include "kernel/Paging.h"
#include "lib/vitralKernel/Ports.h"
#include "kernel/KernelLog.h"

uint16_t
BochsVbe::read(uint16_t reg) {
    Ports::outw(INDEX_PORT, reg);
    return Ports::inw(DATA_PORT);
}

void
BochsVbe::write(uint16_t reg, uint16_t value) {
    Ports::outw(INDEX_PORT, reg);
    Ports::outw(DATA_PORT, value);
}

SvgaDriver *
BochsVbe::probe() {
    class Known {
    public:
        uint16_t vendor;
        uint16_t device;
        const char *who;
    };
    static const Known KNOWN[] = {
        {0x1234, 0x1111, "Bochs / QEMU standard VGA"},
        {0x80EE, 0xBEEF, "VirtualBox graphics adapter"},
    };
    Pci::Location at;
    const Known *found = nullptr;
    for (const Known &k : KNOWN) {
        if (Pci::find(k.vendor, k.device, at)) {
            found = &k;
            break;
        }
    }
    if (!found) {
        return nullptr;
    }

    uint16_t id = read(ID);
    if (id < ID_FIRST || id > ID_LAST) {
        kprintf("Video: %s found but no VBE dispi interface (ID 0x%x)\n", found->who, (uint32_t)id);
        return nullptr;
    }
    uint64_t address;
    uint64_t size;
    if (!Pci::memoryBar(at, 0, address, size) || address + size > Paging::mappedBytes()) {
        kprintf("Video: %s: frame buffer BAR unusable\n", found->who);
        return nullptr;
    }
    Pci::write16(at, Pci::COMMAND, Pci::read16(at, Pci::COMMAND) | Pci::COMMAND_MEMORY | Pci::COMMAND_IO);

    uint64_t memory = id >= ID_MEMORY_SIZE ? (uint64_t)read(VIDEO_MEMORY_64K) * 65536 : 0;
    if (memory == 0 || memory > size) {
        memory = size < (4u << 20) ? size : (4u << 20);
    }
    kprintf("Video: %s (%s), VBE dispi 0x%x, %u KB, frame buffer at 0x%x\n", "Bochs VBE", found->who, (uint32_t)id,
             (uint32_t)(memory / 1024), (uint32_t)address);
    return new BochsVbe((uint32_t)memory, address);
}

bool
BochsVbe::canSet(unsigned width, unsigned height, unsigned bytesPerPixel) const {
    if (width == 0 || height == 0 || width > MAX_WIDTH || height > MAX_HEIGHT) {
        return false;
    }
    if (bytesPerPixel < 1 || bytesPerPixel > 4) {
        return false;
    }
    return (uint64_t)width * height * bytesPerPixel <= memoryBytes;
}

bool
BochsVbe::setMode(unsigned width, unsigned height, unsigned bytesPerPixel) {
    if (!canSet(width, height, bytesPerPixel)) {
        return false;
    }
    write(ENABLE, 0);
    write(XRES, (uint16_t)width);
    write(YRES, (uint16_t)height);
    write(BPP, (uint16_t)(bytesPerPixel * 8));
    write(VIRT_WIDTH, (uint16_t)width);
    write(X_OFFSET, 0);
    write(Y_OFFSET, 0);
    write(ENABLE, ENABLED | LFB_ENABLED);
    if (read(XRES) != width || read(YRES) != height || read(BPP) != bytesPerPixel * 8) {
        write(ENABLE, 0);
        return false;
    }
    pitch = (uint32_t)read(VIRT_WIDTH) * bytesPerPixel;
    return pitch >= width * bytesPerPixel;
}

void
BochsVbe::leaveMode() {
    write(ENABLE, 0);
}

void
BochsVbe::writePixel(unsigned x, unsigned y, unsigned sourceBytesPerPixel, uint32_t color) {
    volatile uint8_t *dst = frame + (uint64_t)y * pitch + (uint64_t)x * sourceBytesPerPixel;
    for (unsigned i = 0; i < sourceBytesPerPixel; i++) {
        dst[i] = (uint8_t)(color >> (8 * i));
    }
}
