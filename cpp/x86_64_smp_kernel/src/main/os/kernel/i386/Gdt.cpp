#include "kernel/Gdt.h"

uint64_t Gdt::table[3] = {
    0,
    0x00CF9A000000FFFF,
    0x00CF92000000FFFF,
};

void
Gdt::load() {
    Pointer p = {sizeof(table) - 1, (uintptr_t)table};
    asm volatile("lgdt %0" : : "m"(p));
    asm volatile("ljmp %0, $1f\n"
                 "1:\n\t"
                 "movw %1, %%ax\n\t"
                 "movw %%ax, %%ds\n\t"
                 "movw %%ax, %%es\n\t"
                 "movw %%ax, %%ss\n\t"
                 "xorl %%eax, %%eax\n\t"
                 "movw %%ax, %%fs\n\t"
                 "movw %%ax, %%gs"
                 : : "i"(CODE), "i"(DATA) : "eax", "memory");
}
