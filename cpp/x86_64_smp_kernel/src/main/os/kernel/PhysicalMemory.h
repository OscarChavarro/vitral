#ifndef __PHYSICAL_MEMORY__
#define __PHYSICAL_MEMORY__
#include <stdint.h>
#include "boot/BootInfo.h"
#include "lib/vitralKernel/SpinLock.h"

class PhysicalMemory {
public:
    static constexpr uint64_t FRAME = 4096;
    static constexpr uint64_t ANY = ~0ull;
    // RAM the kernel can address: all of it in x86_64, the first 4 GB in i386 (no PAE).
    static constexpr uint64_t ADDRESSABLE = sizeof(uintptr_t) < sizeof(uint64_t) ? 1ull << (8 * sizeof(uintptr_t))
                                                                                : ANY;

    static void init(const BootInfo &info);

    static uint64_t allocFrame();
    static uint64_t allocContiguous(uint64_t count, uint64_t below = ANY);
    static void freeFrames(uint64_t address, uint64_t count = 1);

    static uint64_t highestAddress() { return highest; }
    static uint64_t totalFrames() { return usableFrames; }
    static uint64_t freeFrameCount() { return freeCount; }

private:
    static uint64_t *bitmap;
    static uint64_t frames;
    static uint64_t highest;
    static uint64_t usableFrames;
    static uint64_t freeCount;
    static uint64_t hint;
    static SpinLock lock;

    static bool used(uint64_t f) { return bitmap[f >> 6] & (1ull << (f & 63)); }
    static void markUsed(uint64_t f);
    static void markFree(uint64_t f);
    static void reserve(uint64_t start, uint64_t end);
    static uint64_t alignUp(uint64_t v) { return (v + FRAME - 1) & ~(FRAME - 1); }
};

#endif
