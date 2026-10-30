#include "kernel/WallClock.h"
#include "drivers/rtc/Rtc.h"
#include "kernel/Clock.h"
#include "lib/vitralKernel/DateTime.h"

int64_t WallClock::bootSeconds = 0;
uint64_t WallClock::baseNanoseconds = 0;
bool WallClock::rtcWorks = false;
int64_t WallClock::started = 0;
bool WallClock::startKnown = false;

void
WallClock::recordStart() {
    DateTime now;
    startKnown = Rtc::read(now);
    started = startKnown ? Calendar::toUnix(now) : 0;
}

uint64_t
WallClock::uptimeSeconds() {
    DateTime now;
    if (startKnown && Rtc::read(now)) {
        int64_t elapsed = Calendar::toUnix(now) - started;
        return elapsed > 0 ? (uint64_t)elapsed : 0;
    }
    return Clock::uptimeMs() / 1000;
}

void
WallClock::init() {
    DateTime now;
    rtcWorks = Rtc::read(now);
    bootSeconds = rtcWorks ? Calendar::toUnix(now) : 0;
    baseNanoseconds = Clock::nanoseconds();
}

uint64_t
WallClock::nanoseconds() {
    return (uint64_t)bootSeconds * 1000000000ull + (Clock::nanoseconds() - baseNanoseconds);
}

int64_t
WallClock::seconds() { return (int64_t)(nanoseconds() / 1000000000ull); }
