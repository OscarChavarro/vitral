#ifndef __BIOS_DISK__
#define __BIOS_DISK__
#include <stdint.h>

class BiosDisk {
public:
    static constexpr uint32_t BOUNCE_SEGMENT = 0x2000;

    static bool init(uint8_t drive);
    static uint32_t readToBounce(uint32_t lba, uint32_t count);
    static uint32_t bounceRead32(uint16_t offset);

private:
    static uint8_t drive;
    static uint32_t sectorsPerTrack;
    static uint32_t heads;
};

#endif
