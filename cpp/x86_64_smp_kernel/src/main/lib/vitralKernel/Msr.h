#ifndef __MSR__
#define __MSR__
#include <stdint.h>

class Msr {
public:
    static constexpr uint32_t APIC_BASE = 0x1B;
    static constexpr uint32_t EFER = 0xC0000080;

    static uint64_t read(uint32_t msr) {
        uint32_t lo;
        uint32_t hi;
        asm volatile("rdmsr" : "=a"(lo), "=d"(hi) : "c"(msr));
        return ((uint64_t)hi << 32) | lo;
    }
    static void write(uint32_t msr, uint64_t v) {
        asm volatile("wrmsr" : : "c"(msr), "a"((uint32_t)v), "d"((uint32_t)(v >> 32)));
    }
};

#endif
