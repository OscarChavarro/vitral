#include "kernel/LocalApic.h"
#include "kernel/Clock.h"
#include "kernel/Interrupts.h"
#include "kernel/Panic.h"
#include "lib/vitralKernel/Cpu.h"
#include "lib/vitralKernel/Msr.h"

uintptr_t LocalApic::base = 0;
uint32_t LocalApic::countsPerTick = 0;

bool
LocalApic::supported() {
    if (!Cpu::hasApic()) {
        return false;
    }
    uint64_t v = Msr::read(Msr::APIC_BASE);
    return !(v & (1 << 10));
}

void
LocalApic::enable() {
    uint64_t v = Msr::read(Msr::APIC_BASE);
    if (!base) {
        base = (uintptr_t)(v & 0xFFFFF000);
        if (base >= (4ull << 30)) {
            Panic::halt("local APIC above 4 GB is not mapped");
        }
    }
    Msr::write(Msr::APIC_BASE, v | (1 << 11));
    write(REG_SVR, 0x100 | Interrupts::SPURIOUS);
}

uint32_t
LocalApic::id() { return read(REG_ID) >> 24; }

void
LocalApic::eoi() { write(REG_EOI, 0); }

void
LocalApic::calibrate() {
    write(REG_TIMER_DIVIDE, 0x3);
    write(REG_LVT_TIMER, 1 << 16);
    write(REG_TIMER_INIT, 0xFFFFFFFF);
    uint64_t t = Clock::ticks();
    while (Clock::ticks() == t) {
        asm volatile("pause");
    }
    uint32_t c0 = read(REG_TIMER_CURRENT);
    t = Clock::ticks();
    while (Clock::ticks() < t + 4) {
        asm volatile("pause");
    }
    uint32_t c1 = read(REG_TIMER_CURRENT);
    write(REG_TIMER_INIT, 0);
    countsPerTick = (c0 - c1) / 4;
    if (countsPerTick == 0) {
        Panic::halt("local APIC timer does not count");
    }
}

void
LocalApic::initTimer() {
    write(REG_TIMER_DIVIDE, 0x3);
    write(REG_LVT_TIMER, Interrupts::LAPIC_TIMER);
    write(REG_TIMER_INIT, 0);
}

void
LocalApic::armOneShot(uint64_t nanoseconds) {
    const uint64_t pitTickNs = 1000000000000ull / Clock::HZ_MILLI;
    if (nanoseconds > 1000000000ull) {
        nanoseconds = 1000000000ull;
    }
    uint64_t counts = nanoseconds * countsPerTick / pitTickNs;
    write(REG_TIMER_INIT, counts ? (uint32_t)counts : 1);
}

void
LocalApic::stopTimer() { write(REG_TIMER_INIT, 0); }

void
LocalApic::sendIpi(uint32_t apicId, uint32_t low) {
    write(REG_ICR_HIGH, apicId << 24);
    write(REG_ICR_LOW, low);
    while (read(REG_ICR_LOW) & (1 << 12)) {
        asm volatile("pause");
    }
}

void
LocalApic::sendInit(uint32_t apicId) { sendIpi(apicId, 0x4500); }

void
LocalApic::sendStartup(uint32_t apicId, uint8_t page) { sendIpi(apicId, 0x4600 | page); }

void
LocalApic::sendVector(uint32_t apicId, uint8_t vector) { sendIpi(apicId, 0x4000 | vector); }
