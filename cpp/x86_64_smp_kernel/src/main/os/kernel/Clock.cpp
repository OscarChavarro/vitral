#include "kernel/Clock.h"
#include "kernel/Interrupts.h"
#include "kernel/Panic.h"
#include "kernel/Pic.h"
#include "kernel/Scheduler.h"
#include "net/Net.h"
#include "net/Tcp.h"
#include "lib/vitralKernel/Ports.h"

volatile uint64_t Clock::tickCount = 0;
volatile uint64_t Clock::tscAtFirstTick = 0;
volatile uint64_t Clock::tscPerSecond = 0;

static uint64_t
readTsc() {
    uint32_t low;
    uint32_t high;
    asm volatile("rdtsc" : "=a"(low), "=d"(high));
    return ((uint64_t)high << 32) | low;
}

uint64_t
Clock::nanoseconds() {
    uint64_t hz = tscPerSecond;
    if (!hz) {
        return uptimeMs() * 1000000;
    }
    uint64_t delta = readTsc() - tscAtFirstTick;
    uint64_t base = 1000000000000ull / HZ_MILLI;
    return base + (delta / hz) * 1000000000ull + (delta % hz) * 1000000000ull / hz;
}

void
Clock::init() {
    Ports::outb(0x43, 0x34);
    Ports::outb(0x40, 0);
    Ports::outb(0x40, 0);
    if (!Interrupts::registerHandler(Interrupts::IRQ_BASE + 0, onTick)) {
        Panic::halt("timer vector already taken");
    }
    Pic::unmask(0);
}

uint64_t
Clock::msToTicks(uint64_t ms) {
    uint64_t t = (ms * HZ_MILLI + 999999) / 1000000;
    return t ? t : 1;
}

TrapFrame *
Clock::onTick(TrapFrame *frame) {
    tickCount = tickCount + 1;
    if (tickCount == 1) {
        tscAtFirstTick = readTsc();
    } else if (tickCount == 19) {
        tscPerSecond = (readTsc() - tscAtFirstTick) * HZ_MILLI / 18000;
    }
    if (tickCount > 19) {
        Tcp::timer();
        Net::timer();
    }
    return (Scheduler::isRunning() && !Scheduler::usesLapicTimers()) ? Scheduler::tick(frame) : frame;
}
