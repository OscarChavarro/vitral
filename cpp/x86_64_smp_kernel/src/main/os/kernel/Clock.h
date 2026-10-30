#ifndef __CLOCK__
#define __CLOCK__
#include <stdint.h>
#include "kernel/TrapFrame.h"

class Clock {
public:
    static constexpr uint32_t HZ_MILLI = 18206;

    static void init();
    static uint64_t ticks() { return tickCount; }
    static uint64_t uptimeMs() { return tickCount * 1000000 / HZ_MILLI; }
    static uint64_t msToTicks(uint64_t ms);
    static uint64_t nanoseconds();
    static uint64_t cycles() {
        uint32_t low;
        uint32_t high;
        asm volatile("rdtsc" : "=a"(low), "=d"(high));
        return ((uint64_t)high << 32) | low;
    }

private:
    static volatile uint64_t tickCount;
    static volatile uint64_t tscAtFirstTick;
    static volatile uint64_t tscPerSecond;
    static TrapFrame *onTick(TrapFrame *frame);
};

#endif
