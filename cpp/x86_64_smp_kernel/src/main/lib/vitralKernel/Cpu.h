#ifndef __CPU__
#define __CPU__
#include <stdint.h>

class Cpu {
public:
    class Regs {
    public:
        uint32_t eax;
        uint32_t ebx;
        uint32_t ecx;
        uint32_t edx;
    };

    static bool hasCpuid();
    static Regs cpuid(uint32_t leaf, uint32_t subleaf = 0);
    static uint32_t maxLeaf();
    static uint32_t maxExtendedLeaf();

    static bool hasHlt();
    static bool hasFpu();
    static bool hasSse();
    static bool hasMWait();
    static bool hasMsr();
    static bool hasPae();
    static bool hasApic();
    static bool hasSse2();
    static bool hasFxsr();
    static bool hasSse41();
    static bool hasLongMode();
    static bool hasGigabytePages();
    static bool hasPat();
    static bool hasMtrr();

    // Sends out what the CPU holds of its writes to write-combining memory. The i386 build never maps any, and may run
    // on CPUs without this instruction.
    static void flushWrites() {
#if defined(__x86_64__)
        asm volatile("sfence" : : : "memory");
#endif
    }

    static uint32_t apicId();
    static bool topology(unsigned &smtBits, unsigned &coreBits);

private:
    static bool feature(uint32_t leaf, bool inEcx, unsigned bit);
};

#endif
