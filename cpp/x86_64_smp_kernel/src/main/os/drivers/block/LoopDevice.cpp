#include "drivers/block/LoopDevice.h"
#include "fs/Err.h"
#include "lib/vitralKernel/Memory.h"

extern uint8_t loopImageStart[] asm("__loop_image_start");
extern uint8_t loopImageEnd[] asm("__loop_image_end");

uint8_t *
LoopDevice::imageStart() { return loopImageStart; }
uint64_t
LoopDevice::imageBytes() { return (uint64_t)(loopImageEnd - loopImageStart); }

LoopDevice::LoopDevice(const char *deviceName, uint8_t *memory, uint64_t bytes)
    : BlockDevice(deviceName), base(memory), length(bytes) {}

int64_t
LoopDevice::readSectors(uint64_t first, uint32_t count, void *buffer) {
    uint64_t total = sectorCount();
    if (first >= total) {
        return 0;
    }
    if (count > total - first) {
        count = (uint32_t)(total - first);
    }
    Memory::copy(buffer, base + first * SECTOR_SIZE, (size_t)count * SECTOR_SIZE);
    return count;
}

int64_t
LoopDevice::writeSectors(uint64_t first, uint32_t count, const void *buffer) {
    uint64_t total = sectorCount();
    if (first >= total) {
        return -Err::NOSPC;
    }
    if (count > total - first) {
        count = (uint32_t)(total - first);
    }
    Memory::copy(base + first * SECTOR_SIZE, buffer, (size_t)count * SECTOR_SIZE);
    return count;
}
