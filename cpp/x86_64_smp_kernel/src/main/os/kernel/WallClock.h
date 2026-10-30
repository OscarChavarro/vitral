#ifndef __WALL_CLOCK__
#define __WALL_CLOCK__
#include <stdint.h>

class WallClock {
public:
    static void init();
    static bool fromRtc() { return rtcWorks; }
    static void recordStart();
    static int64_t startSeconds() { return started; }
    static uint64_t uptimeSeconds();
    static int64_t seconds();
    static uint64_t nanoseconds();

private:
    static int64_t bootSeconds;
    static uint64_t baseNanoseconds;
    static bool rtcWorks;
    static int64_t started;
    static bool startKnown;
};

#endif
