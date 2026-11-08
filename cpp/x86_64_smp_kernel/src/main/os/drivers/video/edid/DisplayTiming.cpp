#include "drivers/video/edid/DisplayTiming.h"

// VESA DMT 1.13 timings at 60 Hz; the 16:9 ones are CEA-861 (which DMT repeats).
static const DisplayTiming DMT_60_HZ[] = {
    {25175, 640, 656, 752, 800, 480, 490, 492, 525, false, false},
    {40000, 800, 840, 968, 1056, 600, 601, 605, 628, true, true},
    {65000, 1024, 1048, 1184, 1344, 768, 771, 777, 806, false, false},
    {74250, 1280, 1390, 1430, 1650, 720, 725, 730, 750, true, true},
    {83500, 1280, 1352, 1480, 1680, 800, 803, 809, 831, false, true},
    {108000, 1280, 1376, 1488, 1800, 960, 961, 964, 1000, true, true},
    {108000, 1280, 1328, 1440, 1688, 1024, 1025, 1028, 1066, true, true},
    {106500, 1440, 1520, 1672, 1904, 900, 903, 909, 934, false, true},
    {108000, 1600, 1624, 1704, 1800, 900, 901, 904, 1000, true, true},
    {162000, 1600, 1664, 1856, 2160, 1200, 1201, 1204, 1250, true, true},
    {146250, 1680, 1784, 1960, 2240, 1050, 1053, 1059, 1089, false, true},
    {148500, 1920, 2008, 2052, 2200, 1080, 1084, 1089, 1125, true, true},
};

unsigned
DisplayTiming::refreshHz() const {
    uint64_t pixels = (uint64_t)hTotal * vTotal;
    if (pixels == 0) {
        return 0;
    }
    return (unsigned)(((uint64_t)pixelClockKhz * 1000 + pixels / 2) / pixels);
}

bool
DisplayTiming::sane() const {
    return pixelClockKhz != 0 && hDisplay != 0 && vDisplay != 0 && hDisplay <= hSyncStart &&
           hSyncStart < hSyncEnd && hSyncEnd < hTotal && vDisplay <= vSyncStart && vSyncStart < vSyncEnd &&
           vSyncEnd < vTotal;
}

const DisplayTiming *
DisplayTiming::standard(unsigned width, unsigned height) {
    for (const DisplayTiming &t : DMT_60_HZ) {
        if (t.hDisplay == width && t.vDisplay == height) {
            return &t;
        }
    }
    return nullptr;
}
