#include "kernel/Panic.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "kernel/Idle.h"
#include "lib/vitralKernel/Printf.h"

static const char *const EXCEPTION_NAMES[32] = {
    "divide error", "debug", "NMI", "breakpoint", "overflow", "bound range exceeded",
    "invalid opcode", "device not available", "double fault", "coprocessor segment overrun",
    "invalid TSS", "segment not present", "stack-segment fault", "general protection fault",
    "page fault", "reserved", "x87 floating-point error", "alignment check", "machine check",
    "SIMD floating-point error", "virtualization", "control protection", "reserved", "reserved",
    "reserved", "reserved", "reserved", "reserved", "hypervisor injection", "VMM communication",
    "security", "reserved",
};

void
Panic::halt(const char *message) {
    asm volatile("cli");
    VirtualConsole::takeOver();
    VirtualConsole::setAttr(VirtualConsole::active(), 0x4F);
    printf64("\nKERNEL PANIC: %s\n", message);
    Idle::stop();
}

void
Panic::exception(const TrapFrame &f) {
    asm volatile("cli");
    VirtualConsole::takeOver();
    VirtualConsole::setAttr(VirtualConsole::active(), 0x4F);
    printf64("\nKERNEL PANIC: %s (vector %u, error code 0x%x)\n",
             f.vector < 32 ? EXCEPTION_NAMES[f.vector] : "?", (uint32_t)f.vector, (uint32_t)f.errorCode);
    if (f.vector == 14) {
        uintptr_t cr2;
        asm volatile("mov %%cr2, %0" : "=r"(cr2));
        printf64("  faulting address CR2=0x%x%08x\n", (uint32_t)((uint64_t)cr2 >> 32), (uint32_t)cr2);
    }
    printRegisters(f);
    Idle::stop();
}
