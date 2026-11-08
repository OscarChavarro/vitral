#ifndef __EDID__
#define __EDID__
#include <stdint.h>
#include "drivers/video/edid/DisplayTiming.h"

// The base block of a monitor's EDID (VESA E-EDID 1.3 / 1.4), read by a video driver through the DDC bus: who the
// monitor is and which modes it accepts. Extension blocks are not read.
class Edid {
public:
    static constexpr unsigned BLOCK_BYTES = 128;
    static constexpr unsigned MAX_DETAILED = 4;
    static constexpr uint8_t DDC_ADDRESS = 0x50;

    // False when the block lacks the header or its checksum does not add up.
    bool parse(const uint8_t *block);
    bool valid() const { return isValid; }
    // Three letter PNP manufacturer code, the monitor name of its descriptor (empty when it has none).
    const char *vendor() const { return vendorCode; }
    uint16_t product() const { return productCode; }
    const char *name() const { return monitorName; }
    bool analogInput() const { return analog; }
    unsigned detailedCount() const { return detailed; }
    // The detailed timings; the first is the monitor's preferred (native) mode.
    const DisplayTiming &detailedTiming(unsigned i) const { return timings[i]; }
    // A detailed timing of this size, the earliest one listed, or null.
    const DisplayTiming *detailedFor(unsigned width, unsigned height) const;
    // Whether the monitor says it takes this timing: one of its detailed timings, a size and refresh it lists among
    // the established or standard timings, or one inside its range limits.
    bool supports(const DisplayTiming &timing) const;

private:
    static constexpr unsigned MAX_STANDARD = 8;
    static constexpr unsigned NAME_BYTES = 14;

    class ListedMode {
    public:
        uint16_t width;
        uint16_t height;
        uint8_t refresh;
    };

    bool isValid = false;
    char vendorCode[4] = {0, 0, 0, 0};
    uint16_t productCode = 0;
    char monitorName[NAME_BYTES] = {0};
    bool analog = false;
    unsigned detailed = 0;
    DisplayTiming timings[MAX_DETAILED];
    uint8_t established[3] = {0, 0, 0};
    unsigned standardCount = 0;
    ListedMode standardModes[MAX_STANDARD];
    bool hasRange = false;
    unsigned minVerticalHz = 0;
    unsigned maxVerticalHz = 0;
    unsigned minHorizontalKhz = 0;
    unsigned maxHorizontalKhz = 0;
    uint32_t maxPixelClockKhz = 0;

    void parseDescriptor(const uint8_t *d);
    bool listed(unsigned width, unsigned height, unsigned refresh) const;
};

#endif
