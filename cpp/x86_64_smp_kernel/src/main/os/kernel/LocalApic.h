#ifndef __LOCAL_APIC__
#define __LOCAL_APIC__
#include <stdint.h>

class LocalApic {
public:
    static bool supported();
    static void enable();
    static bool isEnabled() { return base != 0; }
    static uint32_t id();
    static void eoi();

    static void calibrate();
    static void initTimer();
    static void armOneShot(uint64_t nanoseconds);
    static void stopTimer();

    static void sendInit(uint32_t apicId);
    static void sendStartup(uint32_t apicId, uint8_t page);
    static void sendVector(uint32_t apicId, uint8_t vector);

private:
    static constexpr uint32_t REG_ID = 0x20;
    static constexpr uint32_t REG_EOI = 0xB0;
    static constexpr uint32_t REG_SVR = 0xF0;
    static constexpr uint32_t REG_ICR_LOW = 0x300;
    static constexpr uint32_t REG_ICR_HIGH = 0x310;
    static constexpr uint32_t REG_LVT_TIMER = 0x320;
    static constexpr uint32_t REG_TIMER_INIT = 0x380;
    static constexpr uint32_t REG_TIMER_CURRENT = 0x390;
    static constexpr uint32_t REG_TIMER_DIVIDE = 0x3E0;

    static uintptr_t base;
    static uint32_t countsPerTick;

    static uint32_t read(uint32_t reg) { return *(volatile uint32_t *)(base + reg); }
    static void write(uint32_t reg, uint32_t v) { *(volatile uint32_t *)(base + reg) = v; }
    static void sendIpi(uint32_t apicId, uint32_t low);
};

#endif
