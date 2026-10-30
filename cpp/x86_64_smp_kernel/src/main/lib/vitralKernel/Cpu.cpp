#include "lib/vitralKernel/Cpu.h"

bool
Cpu::hasCpuid() {
#if defined(__x86_64__)
    return true;
#else
    uint32_t orig;
    uint32_t flipped;
    asm volatile("pushfl\n\t"
                 "popl %0\n\t"
                 "movl %0, %1\n\t"
                 "xorl $0x200000, %1\n\t"
                 "pushl %1\n\t"
                 "popfl\n\t"
                 "pushfl\n\t"
                 "popl %1\n\t"
                 "pushl %0\n\t"
                 "popfl"
                 : "=&r"(orig), "=&r"(flipped) : : "cc", "memory");
    return (orig ^ flipped) & 0x200000;
#endif
}

Cpu::Regs
Cpu::cpuid(uint32_t leaf, uint32_t subleaf) {
    Regs r;
    asm volatile("cpuid" : "=a"(r.eax), "=b"(r.ebx), "=c"(r.ecx), "=d"(r.edx)
                         : "a"(leaf), "c"(subleaf));
    return r;
}

uint32_t
Cpu::maxLeaf() { return hasCpuid() ? cpuid(0).eax : 0; }
uint32_t
Cpu::maxExtendedLeaf() { return hasCpuid() ? cpuid(0x80000000).eax : 0; }

bool
Cpu::feature(uint32_t leaf, bool inEcx, unsigned bit) {
    if (!hasCpuid()) {
        return false;
    }
    uint32_t max = (leaf & 0x80000000) ? maxExtendedLeaf() : maxLeaf();
    if (max < leaf) {
        return false;
    }
    Regs r = cpuid(leaf);
    return ((inEcx ? r.ecx : r.edx) >> bit) & 1;
}

bool
Cpu::hasHlt() { return true; }
bool
Cpu::hasMWait() { return maxLeaf() >= 5 && feature(1, true, 3); }
bool
Cpu::hasFpu() { return feature(1, false, 0); }
bool
Cpu::hasSse() { return feature(1, false, 25); }
bool
Cpu::hasMsr() { return feature(1, false, 5); }
bool
Cpu::hasPae() { return feature(1, false, 6); }
bool
Cpu::hasApic() { return feature(1, false, 9); }
bool
Cpu::hasSse2() { return feature(1, false, 26); }
bool
Cpu::hasFxsr() { return feature(1, false, 24); }
bool
Cpu::hasSse41() { return feature(1, true, 19); }
bool
Cpu::hasLongMode() { return feature(0x80000001, false, 29); }
bool
Cpu::hasGigabytePages() { return feature(0x80000001, false, 26); }

uint32_t
Cpu::apicId() {
    if (maxLeaf() >= 0xB) {
        return cpuid(0xB).edx;
    }
    return cpuid(1).ebx >> 24;
}

bool
Cpu::topology(unsigned &smtBits, unsigned &coreBits) {
    if (maxLeaf() < 0xB || cpuid(0xB, 0).ebx == 0) {
        return false;
    }
    smtBits = 0;
    coreBits = 0;
    for (uint32_t level = 0; level < 8; level++) {
        Regs r = cpuid(0xB, level);
        unsigned type = (r.ecx >> 8) & 0xFF;
        unsigned shift = r.eax & 0x1F;
        if (type == 0) {
            break;
        }
        if (type == 1) {
            smtBits = shift;
        }
        if (type == 2) {
            coreBits = shift - smtBits;
        }
    }
    return true;
}
