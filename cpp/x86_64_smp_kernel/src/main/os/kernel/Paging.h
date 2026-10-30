#ifndef __PAGING__
#define __PAGING__
#include <stdint.h>
#include "boot/BootInfo.h"

// Identity map of the physical address space. x86_64 builds 4-level tables (long mode needs paging); the i386 build
// runs with paging off, so every 32-bit physical address is reachable as it is.
class Paging {
public:
    static void init(const BootInfo &info);
    static uint64_t root() { return rootAddress; }
    static uint64_t mappedBytes() { return mappedEnd; }
    static uint64_t tableFrames() { return tables; }

private:
    static constexpr uint64_t PRESENT = 1;
    static constexpr uint64_t WRITABLE = 2;
    static constexpr uint64_t WRITE_THROUGH = 8;
    static constexpr uint64_t CACHE_DISABLE = 0x10;
    static constexpr uint64_t HUGE = 0x80;
    static constexpr uint64_t UNCACHED = WRITE_THROUGH | CACHE_DISABLE;
    static constexpr uint64_t MB2 = 2ull << 20;
    static constexpr uint64_t GB1 = 1ull << 30;

    static uint64_t rootAddress;
    static uint64_t mappedEnd;
    static uint64_t tables;

    static uint64_t *newTable(uint64_t below);
    static bool isMemory(const BootInfo &info, uint64_t start, uint64_t end);
};

#endif
