#ifndef __CLOCK__
#define __CLOCK__
#include <stdint.h>
#include "kernel/TrapFrame.h"

class Clock {
public:
    static constexpr uint32_t HZ_MILLI = 18206;

    static void init();
    // The PIT interrupts counted so far. Only CPU 0 takes them, so the count stands still for code that runs there
    // with interrupts off (a syscall, an interrupt handler, a spin lock held): measure waits with uptimeMs().
    static uint64_t ticks() { return tickCount; }
    // From the TSC once it is calibrated (about a second after the timer starts), so that it advances on every CPU
    // with interrupts off too; from the ticks before.
    static uint64_t uptimeMs() { return nanoseconds() / 1000000; }
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
