#include "kernel/Pic.h"
#include "kernel/Interrupts.h"
#include "lib/vitralKernel/Ports.h"

static constexpr uint16_t MASTER = 0x20;
static constexpr uint16_t SLAVE = 0xA0;

void
Pic::init() {
    Ports::outb(MASTER, 0x11);
    Ports::outb(SLAVE, 0x11);
    Ports::outb(MASTER + 1, Interrupts::IRQ_BASE);
    Ports::outb(SLAVE + 1, Interrupts::IRQ_BASE + 8);
    Ports::outb(MASTER + 1, 4); Ports::outb(SLAVE + 1, 2);
    Ports::outb(MASTER + 1, 0x01); Ports::outb(SLAVE + 1, 0x01);
    disable();
}

void
Pic::disable() {
    Ports::outb(MASTER + 1, 0xFF);
    Ports::outb(SLAVE + 1, 0xFF);
}

void
Pic::mask(unsigned irq) {
    uint16_t port = (irq < 8 ? MASTER : SLAVE) + 1;
    Ports::outb(port, Ports::inb(port) | (1 << (irq & 7)));
}

void
Pic::unmask(unsigned irq) {
    uint16_t port = (irq < 8 ? MASTER : SLAVE) + 1;
    Ports::outb(port, Ports::inb(port) & ~(1 << (irq & 7)));
    if (irq >= 8) {
        unmask(2);
    }
}

uint16_t
Pic::readIsr() {
    Ports::outb(MASTER, 0x0B);
    Ports::outb(SLAVE, 0x0B);
    return (uint16_t)(Ports::inb(SLAVE) << 8) | Ports::inb(MASTER);
}

void
Pic::eoi(unsigned irq) {
    if ((irq == 7 || irq == 15) && !(readIsr() & (1 << irq))) {
        if (irq == 15) {
            Ports::outb(MASTER, 0x20);
        }
        return;
    }
    if (irq >= 8) {
        Ports::outb(SLAVE, 0x20);
    }
    Ports::outb(MASTER, 0x20);
}
