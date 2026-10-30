#ifndef __GDT__
#define __GDT__
#include <stdint.h>

class Gdt {
public:
    static constexpr uint16_t CODE = 0x08;
    static constexpr uint16_t DATA = 0x10;

    static void load();

private:
    class Pointer {
    public:
        uint16_t limit;
        uintptr_t base;
    } __attribute__((packed));
    static uint64_t table[3];
};

#endif
