#include "kernel/Fpu.h"
#include "kernel/Panic.h"
#include "lib/vitralKernel/Cpu.h"
#include "lib/vitralKernel/Memory.h"

void
Fpu::enable() {
    if (!Cpu::hasFxsr() || !Cpu::hasSse2()) {
        Panic::halt("this CPU has no FXSAVE/SSE2");
    }
    uint64_t cr0;
    uint64_t cr4;
    asm volatile("mov %%cr0, %0" : "=r"(cr0));
    cr0 &= ~(1ull << 2);
    cr0 |= 1ull << 1;
    cr0 &= ~(1ull << 3);
    asm volatile("mov %0, %%cr0" : : "r"(cr0));
    asm volatile("mov %%cr4, %0" : "=r"(cr4));
    cr4 |= (1ull << 9) | (1ull << 10);
    asm volatile("mov %0, %%cr4" : : "r"(cr4));
    asm volatile("fninit");
}

void
Fpu::initialize(Image &image) {
    Memory::set(&image, 0, sizeof(image));
    *(uint16_t *)&image.bytes[0] = 0x037F;
    *(uint32_t *)&image.bytes[24] = 0x1F80;
}

void
Fpu::save(Image &image) { asm volatile("fxsave %0" : "=m"(image) : : "memory"); }

void
Fpu::restore(const Image &image) { asm volatile("fxrstor %0" : : "m"(image) : "memory"); }
