#include "kernel/Paging.h"
#include "kernel/KernelLog.h"
#include "lib/vitralKernel/Cpu.h"
#include "lib/vitralKernel/InterruptsOff.h"
#include "lib/vitralKernel/Msr.h"

uint64_t Paging::rootAddress = 0;
uint64_t Paging::mappedEnd = 0;
uint64_t Paging::tables = 0;
unsigned Paging::patCpus = 0;

// With paging off there are no page attributes: the memory type of an address comes from the MTRRs alone, so a
// variable range of type write-combining over the frame buffer selects it. Every CPU must have the same MTRRs, so
// initCpu() repeats on the other CPUs the pair mapWriteCombining() wrote on this one.
static constexpr uint32_t MTRR_CAP = 0xFE;
static constexpr uint32_t MTRR_DEF_TYPE = 0x2FF;
static constexpr uint32_t MTRR_PHYS_BASE_0 = 0x200;
static constexpr uint64_t MTRR_CAP_COUNT = 0xFF;
static constexpr uint64_t MTRR_CAP_WC = 1u << 10;
static constexpr uint64_t MTRR_ENABLE = 1u << 11;
static constexpr uint64_t MTRR_VALID = 1u << 11;
static constexpr uint64_t MTRR_TYPE = 0xFF;
static constexpr uint64_t MTRR_TYPE_WC = 1;
static constexpr uint32_t CR0_CACHE_DISABLE = 1u << 30;

static int mtrrPair = -1;
static uint64_t mtrrBase;
static uint64_t mtrrMask;

static uint64_t
physicalAddressMask() {
    unsigned bits = Cpu::hasPae() ? 36 : 32;
    if (Cpu::maxExtendedLeaf() >= 0x80000008) {
        bits = Cpu::cpuid(0x80000008).eax & 0xFF;
    }
    return (bits >= 64 ? ~0ull : (1ull << bits) - 1) & ~0xFFFull;
}

// Intel SDM vol. 3, "MTRR Considerations in MP Systems": the caches stay off and empty, and the MTRRs disabled, while
// a pair changes.
static void
writeMtrrPair(unsigned pair, uint64_t base, uint64_t mask) {
    InterruptsOff guard;
    uint32_t control;
    asm volatile("movl %%cr0, %0" : "=r"(control));
    asm volatile("movl %0, %%cr0; wbinvd" : : "r"(control | CR0_CACHE_DISABLE) : "memory");
    uint64_t defaultType = Msr::read(MTRR_DEF_TYPE);
    Msr::write(MTRR_DEF_TYPE, defaultType & ~MTRR_ENABLE);
    Msr::write(MTRR_PHYS_BASE_0 + 2 * pair, base);
    Msr::write(MTRR_PHYS_BASE_0 + 2 * pair + 1, mask);
    asm volatile("wbinvd" : : : "memory");
    Msr::write(MTRR_DEF_TYPE, defaultType);
    asm volatile("movl %0, %%cr0" : : "r"(control) : "memory");
}

static bool
hasMtrr() {
    return Cpu::hasMsr() && Cpu::hasMtrr();
}

void
Paging::init(const BootInfo &) {
    mappedEnd = 4 * GB1;
}

void
Paging::initCpu() {
    if (mtrrPair < 0 || !hasMtrr()) {
        return;
    }
    writeMtrrPair((unsigned)mtrrPair, mtrrBase, mtrrMask);
    if (Msr::read(MTRR_PHYS_BASE_0 + 2 * mtrrPair) == mtrrBase &&
        Msr::read(MTRR_PHYS_BASE_0 + 2 * mtrrPair + 1) == mtrrMask) {
        __atomic_add_fetch(&patCpus, 1, __ATOMIC_RELAXED);
    }
}

// A variable range is a power of two aligned to its size: the largest one that starts at base and fits in the buffer.
// The range must not overlap another one, where the stricter type (uncached, or undefined for write-back) would win.
bool
Paging::mapWriteCombining(uint64_t base, uint64_t bytes) {
    if (bytes == 0 || base + bytes > mappedEnd || mtrrPair >= 0) {
        return false;
    }
    if (!hasMtrr()) {
        kprintf("Paging: no MTRRs, frame buffer left uncached\n");
        return false;
    }
    uint64_t capabilities = Msr::read(MTRR_CAP);
    uint64_t defaultType = Msr::read(MTRR_DEF_TYPE);
    if (!(capabilities & MTRR_CAP_WC) || !(defaultType & MTRR_ENABLE)) {
        kprintf("Paging: MTRRs %s, frame buffer left uncached\n",
                capabilities & MTRR_CAP_WC ? "disabled" : "without write-combining");
        return false;
    }
    uint64_t size = 1;
    while (size * 2 <= bytes) {
        size *= 2;
    }
    while (size > 4096 && (base & (size - 1))) {
        size /= 2;
    }
    if (base & (size - 1) || size < 4096) {
        return false;
    }
    uint64_t addressMask = physicalAddressMask();
    uint64_t mask = ~(size - 1) & addressMask;
    unsigned count = (unsigned)(capabilities & MTRR_CAP_COUNT);
    int free = -1;
    for (unsigned pair = 0; pair < count; pair++) {
        uint64_t otherMask = Msr::read(MTRR_PHYS_BASE_0 + 2 * pair + 1);
        if (!(otherMask & MTRR_VALID)) {
            if (free < 0) {
                free = (int)pair;
            }
            continue;
        }
        uint64_t otherBase = Msr::read(MTRR_PHYS_BASE_0 + 2 * pair);
        otherMask &= addressMask;
        if (((otherBase ^ base) & mask & otherMask) != 0) {
            continue;
        }
        if ((otherBase & MTRR_TYPE) == MTRR_TYPE_WC && (otherBase & otherMask) == (base & otherMask) &&
            (otherMask & ~mask) == 0) {
            mtrrPair = (int)pair;
            mtrrBase = Msr::read(MTRR_PHYS_BASE_0 + 2 * pair);
            mtrrMask = Msr::read(MTRR_PHYS_BASE_0 + 2 * pair + 1);
            __atomic_add_fetch(&patCpus, 1, __ATOMIC_RELAXED);
            return true;
        }
        kprintf("Paging: MTRR %u (base %08x%08x mask %08x%08x) covers the frame buffer, left uncached\n", pair,
                (uint32_t)(otherBase >> 32), (uint32_t)otherBase, (uint32_t)(otherMask >> 32), (uint32_t)otherMask);
        return false;
    }
    if (free < 0) {
        kprintf("Paging: all %u variable MTRRs in use, frame buffer left uncached\n", count);
        return false;
    }
    mtrrPair = free;
    mtrrBase = (base & addressMask) | MTRR_TYPE_WC;
    mtrrMask = mask | MTRR_VALID;
    initCpu();
    kprintf("Paging: MTRR %u: %u MB at %08x write-combining\n", (unsigned)free, (uint32_t)(size >> 20),
            (uint32_t)base);
    return patCpus > 0;
}
