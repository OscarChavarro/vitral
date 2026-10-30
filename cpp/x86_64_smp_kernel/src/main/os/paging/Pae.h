#ifndef __PAE__
#define __PAE__
#include <stdint.h>

class Pae {
public:
    static void build();
    static void activate();
    static uint32_t root();

private:
    static constexpr uint64_t PRESENT = 1;
    static constexpr uint64_t WRITABLE = 2;
    static constexpr uint64_t PAGE_SIZE_2M = 1 << 7;
    static constexpr int PD_COUNT = 4;

    class alignas(4096) Table {
    public:
        uint64_t e[512];
    };
    static Table pml4;
    static Table pdpt;
    static Table pd[PD_COUNT];

    static void zero(Table &t);
    static uint32_t addr(const void *p) { return (uint32_t)(uintptr_t)p; }
};

#endif
