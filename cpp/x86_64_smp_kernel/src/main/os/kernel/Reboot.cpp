#include "kernel/Reboot.h"
#include "acpi/Acpi.h"
#include "kernel/KernelLog.h"
#include "lib/vitralKernel/Ports.h"

// An IDT with no entries: the next interrupt finds no handler, then no double fault handler, and the CPU resets.
struct __attribute__((packed)) RebootEmptyIdt {
    uint16_t limit;
    uintptr_t base;
};

void
Reboot::restart() {
    kprintf("reboot: restarting the machine\n");
    asm volatile("cli");
    uint16_t port = 0;
    uint8_t value = 0;
    if (Acpi::resetRegister(port, value)) {
        Ports::outb(port, value);
        delay(SETTLE_US);
    }
    keyboardController();
    delay(SETTLE_US);
    pciResetPort();
    delay(SETTLE_US);
    tripleFault();
}

void
Reboot::delay(unsigned microseconds) {
    for (unsigned i = 0; i < microseconds; i++) {
        Ports::outb(0x80, 0);
    }
}

void
Reboot::keyboardController() {
    for (unsigned i = 0; i < 100000 && (Ports::inb(KBC_STATUS) & KBC_INPUT_FULL); i++) {
        delay(1);
    }
    Ports::outb(KBC_STATUS, KBC_PULSE_RESET);
}

// Keeps the chipset's other bits; a 0 to 1 change of the CPU reset bit starts the reset, the system bit makes it full.
void
Reboot::pciResetPort() {
    uint8_t keep = Ports::inb(PCI_RESET) & (uint8_t)~(PCI_RESET_SYSTEM | PCI_RESET_CPU);
    Ports::outb(PCI_RESET, keep | PCI_RESET_SYSTEM);
    delay(50);
    Ports::outb(PCI_RESET, keep | PCI_RESET_SYSTEM | PCI_RESET_CPU);
}

void
Reboot::tripleFault() {
    static const RebootEmptyIdt empty = {0, 0};
    asm volatile("lidt %0\n\tint3" : : "m"(empty));
    for (;;) {
        asm volatile("hlt");
    }
}
