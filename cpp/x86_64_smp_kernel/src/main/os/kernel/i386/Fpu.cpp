#include "kernel/Fpu.h"
#include "kernel/Panic.h"
#include "lib/vitralKernel/Cpu.h"
#include "lib/vitralKernel/Memory.h"

// FXSAVE when the CPU has it (Pentium II and later), FSAVE otherwise (Pentium, Pentium MMX).
static bool fxsr = false;

void
Fpu::enable() {
    if (!Cpu::hasFpu()) {
        Panic::halt("this CPU has no x87 FPU");
    }
    fxsr = Cpu::hasFxsr();
    uint32_t cr0;
    asm volatile("mov %%cr0, %0" : "=r"(cr0));
    cr0 &= ~(1u << 2);
    cr0 |= 1u << 1;
    cr0 &= ~(1u << 3);
    cr0 |= 1u << 5;
    asm volatile("mov %0, %%cr0" : : "r"(cr0));
    if (fxsr) {
        uint32_t cr4;
        asm volatile("mov %%cr4, %0" : "=r"(cr4));
        cr4 |= 1u << 9;
        if (Cpu::hasSse()) {
            cr4 |= 1u << 10;
        }
        asm volatile("mov %0, %%cr4" : : "r"(cr4));
    }
    asm volatile("fninit");
}

// Programs start with the x87 rounding to 53 bits (precision control = double): without SSE2, double arithmetic is done
// on the x87, and this makes it round like the SSE2 code of the x86_64 build instead of carrying 64-bit mantissas.
void
Fpu::initialize(Image &image) {
    Memory::set(&image, 0, sizeof(image));
    *(uint16_t *)&image.bytes[0] = 0x027F;
    if (fxsr) {
        *(uint32_t *)&image.bytes[24] = 0x1F80;
    } else {
        *(uint16_t *)&image.bytes[8] = 0xFFFF;
    }
}

void
Fpu::save(Image &image) {
    if (fxsr) {
        asm volatile("fxsave %0" : "=m"(image) : : "memory");
    } else {
        asm volatile("fnsave %0; fwait" : "=m"(image) : : "memory");
    }
}

void
Fpu::restore(const Image &image) {
    if (fxsr) {
        asm volatile("fxrstor %0" : : "m"(image) : "memory");
    } else {
        asm volatile("frstor %0" : : "m"(image) : "memory");
    }
}
