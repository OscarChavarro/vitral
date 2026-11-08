#ifndef __DISPLAY_TIMING__
#define __DISPLAY_TIMING__
#include <stdint.h>

// The timing of a progressive video mode as a monitor sees it: pixel clock, and the visible, sync and total extents
// of a line (in pixels) and of a frame (in lines). Drivers that program a CRTC take it from the monitor's EDID or, for
// the usual sizes, from the VESA DMT table that standard() gives.
class DisplayTiming {
public:
    uint32_t pixelClockKhz;
    uint16_t hDisplay;
    uint16_t hSyncStart;
    uint16_t hSyncEnd;
    uint16_t hTotal;
    uint16_t vDisplay;
    uint16_t vSyncStart;
    uint16_t vSyncEnd;
    uint16_t vTotal;
    bool hSyncPositive;
    bool vSyncPositive;

    // Line frequency in Hz and refresh rate in Hz, rounded.
    uint32_t lineHz() const { return hTotal ? (uint32_t)((uint64_t)pixelClockKhz * 1000 / hTotal) : 0; }
    unsigned refreshHz() const;
    bool sane() const;

    // The VESA DMT timing of a width x height mode at 60 Hz (CEA-861 for 1280x720 and 1920x1080), or null.
    static const DisplayTiming *standard(unsigned width, unsigned height);
};

#endif
