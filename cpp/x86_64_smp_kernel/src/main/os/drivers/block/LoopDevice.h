#ifndef __LOOP_DEVICE__
#define __LOOP_DEVICE__
#include "drivers/block/BlockDevice.h"

class LoopDevice : public BlockDevice {
public:
    static constexpr uint32_t SECTOR_SIZE = 512;

    LoopDevice(const char *deviceName, uint8_t *memory, uint64_t bytes);

    uint32_t sectorSize() const override { return SECTOR_SIZE; }
    uint64_t sectorCount() const override { return length / SECTOR_SIZE; }
    int64_t readSectors(uint64_t first, uint32_t count, void *buffer) override;
    int64_t writeSectors(uint64_t first, uint32_t count, const void *buffer) override;

    static uint8_t *imageStart();
    static uint64_t imageBytes();

private:
    uint8_t *base;
    uint64_t length;
};

#endif
