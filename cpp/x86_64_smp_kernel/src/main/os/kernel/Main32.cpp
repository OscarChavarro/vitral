#include "boot/BootInfo.h"
#include "lib/vitralKernel/Cpu.h"
#include "lib/vitralKernel/Printf.h"
#include "drivers/video/vga/VgaText.h"
#include "paging/Pae.h"

[[noreturn]] void
enter_long_mode(uint32_t entry);

[[noreturn]] static void
halt() {
    for (;;) {
        asm volatile("cli; hlt");
    }
}

[[noreturn]] static void
fatal(const char *why) {
    printf32("CPU ERROR: %s\n", why);
    halt();
}

[[noreturn]] void
kmain32() {
    VgaText::init();
    printf32("PROTECTED MODE 32-BIT\n");

    if (!Cpu::hasCpuid()) {
        fatal("no CPUID (80386/early 80486): cannot use PAE nor long mode x86_64.");
    }
    if (!Cpu::hasPae()) {
        fatal("PAE not supported: cannot build the paging tables for long mode.");
    }
    if (!Cpu::hasLongMode()) {
        printf32("CPU ERROR: long mode x86_64 not supported (needs AMD64 / Intel 64 CPU).\n");
        printf32("This project has a 32-bit build; use it instead of running the x86_64 build on a "
                 "32-bit machine.\n");
        halt();
    }
    if (!Cpu::hasMsr()) {
        fatal("no MSRs: cannot set EFER.LME.");
    }

    Pae::build();
    Pae::activate();
    printf32("PAE PAGING READY: PML4=%p, identity map 4GB (2MB pages)\n", (void *)Pae::root());
    printf32("Entering long mode...\n");
    enter_long_mode(BootInfo::get().kernelEntry);
}
