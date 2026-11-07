#include "drivers/char/uart/SerialTty.h"
#include "drivers/char/CharDevices.h"
#include "drivers/char/uart/Uart.h"
#include "kernel/KernelLog.h"

void
SerialTty::init() {
    if (!Uart::enableReceive()) {
        return;
    }
    if (!CharDevices::add(new SerialTty())) {
        kprintf("COM1: cannot register /dev/ttyS0\n");
        return;
    }
    kprintf("COM1: terminal /dev/ttyS0 ready, IRQ 4\n");
}

size_t
SerialTty::fetch(uint8_t *buffer, size_t size) { return Uart::read((char *)buffer, size); }

void
SerialTty::transmit(const char *bytes, size_t count) { Uart::writeRaw(bytes, count); }
