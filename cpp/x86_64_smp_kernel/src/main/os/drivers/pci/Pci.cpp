#include "drivers/pci/Pci.h"
#include "lib/vitralKernel/Ports.h"

uint32_t
Pci::address(const Location &at, uint8_t offset) {
    return 0x80000000u | (uint32_t)at.bus << 16 | (uint32_t)(at.device & 31) << 11 |
           (uint32_t)(at.function & 7) << 8 | (offset & 0xFC);
}

uint32_t
Pci::read32(const Location &at, uint8_t offset) {
    Ports::outl(ADDRESS_PORT, address(at, offset));
    return Ports::inl(DATA_PORT);
}

uint8_t
Pci::read8(const Location &at, uint8_t offset) {
    return (uint8_t)(read32(at, offset) >> ((offset & 3) * 8));
}

uint16_t
Pci::read16(const Location &at, uint8_t offset) {
    return (uint16_t)(read32(at, offset) >> ((offset & 2) * 8));
}

void
Pci::write32(const Location &at, uint8_t offset, uint32_t value) {
    Ports::outl(ADDRESS_PORT, address(at, offset));
    Ports::outl(DATA_PORT, value);
}

void
Pci::write16(const Location &at, uint8_t offset, uint16_t value) {
    unsigned shift = (offset & 2) * 8;
    uint32_t word = read32(at, offset) & ~(0xFFFFu << shift);
    write32(at, offset, word | (uint32_t)value << shift);
}

bool
Pci::find(uint16_t vendor, uint16_t device, Location &found) {
    for (unsigned bus = 0; bus < 256; bus++) {
        for (unsigned dev = 0; dev < 32; dev++) {
            Location at = {(uint8_t)bus, (uint8_t)dev, 0};
            if (read16(at, VENDOR_ID) == 0xFFFF) {
                continue;
            }
            unsigned functions = (read32(at, HEADER_TYPE & 0xFC) >> 16 & 0x80) ? 8 : 1;
            for (unsigned fn = 0; fn < functions; fn++) {
                at.function = (uint8_t)fn;
                if (read16(at, VENDOR_ID) == vendor && read16(at, DEVICE_ID) == device) {
                    found = at;
                    return true;
                }
            }
        }
    }
    return false;
}

bool
Pci::memoryBar(const Location &at, unsigned index, uint64_t &address, uint64_t &size) {
    if (index > 5) {
        return false;
    }
    uint8_t offset = (uint8_t)(BAR0 + index * 4);
    uint32_t low = read32(at, offset);
    if (low & 1) {
        return false;
    }
    bool wide = (low & 6) == 4;
    if (wide && index == 5) {
        return false;
    }
    uint32_t high = wide ? read32(at, (uint8_t)(offset + 4)) : 0;

    uint16_t command = read16(at, COMMAND);
    write16(at, COMMAND, (uint16_t)(command & ~(COMMAND_IO | COMMAND_MEMORY)));
    write32(at, offset, 0xFFFFFFFFu);
    uint32_t lowMask = read32(at, offset);
    write32(at, offset, low);
    uint32_t highMask = 0xFFFFFFFFu;
    if (wide) {
        write32(at, (uint8_t)(offset + 4), 0xFFFFFFFFu);
        highMask = read32(at, (uint8_t)(offset + 4));
        write32(at, (uint8_t)(offset + 4), high);
    }
    write16(at, COMMAND, command);

    uint64_t mask = ((uint64_t)highMask << 32 | (lowMask & 0xFFFFFFF0u));
    if ((lowMask & 0xFFFFFFF0u) == 0 && (!wide || highMask == 0)) {
        return false;
    }
    size = ~mask + 1;
    if (!wide) {
        size &= 0xFFFFFFFFull;
    }
    address = (uint64_t)high << 32 | (low & 0xFFFFFFF0u);
    return address != 0 && size != 0;
}

bool
Pci::enableMsi(const Location &at, uint8_t vector, uint32_t apicId) {
    if (!(read16(at, STATUS) & (1u << 4)) || apicId > 255) {
        return false;
    }
    uint8_t capability = read8(at, 0x34) & ~3u;
    for (unsigned count = 0; capability >= 0x40 && count < 48; count++) {
        uint8_t id = read8(at, capability);
        uint8_t next = read8(at, (uint8_t)(capability + 1)) & ~3u;
        if (id == 5) {
            uint16_t control = read16(at, (uint8_t)(capability + 2));
            bool wide = control & (1u << 7);
            uint8_t data = (uint8_t)(capability + (wide ? 12 : 8));
            write16(at, (uint8_t)(capability + 2), control & ~1u);
            write32(at, (uint8_t)(capability + 4), 0xFEE00000u | apicId << 12);
            if (wide) {
                write32(at, (uint8_t)(capability + 8), 0);
            }
            write16(at, data, vector);
            if (control & (1u << 8)) {
                write32(at, (uint8_t)(data + 2), 0);
            }
            write16(at, (uint8_t)(capability + 2), control | 1u);
            return true;
        }
        capability = next;
    }
    return false;
}
