#include "drivers/char/uart/Uart.h"
#include "lib/vitralKernel/Ports.h"

bool Uart::enabled = false;

void
Uart::initCom1() {
    Ports::outb(COM1 + INTERRUPT_ENABLE, 0);
    Ports::outb(COM1 + LINE_CONTROL, DLAB);
    Ports::outb(COM1 + DATA, DIVISOR_9600);
    Ports::outb(COM1 + INTERRUPT_ENABLE, 0);
    Ports::outb(COM1 + LINE_CONTROL, 0x03);
    Ports::outb(COM1 + FIFO_CONTROL, 0xC7);
    Ports::outb(COM1 + MODEM_CONTROL, 0x0B);
    enabled = true;
}

bool
Uart::waitForTransmitter() {
    for (unsigned i = 0; i < WAIT_LIMIT; i++) {
        if (Ports::inb(COM1 + LINE_STATUS) & TRANSMITTER_EMPTY) {
            return true;
        }
        asm volatile("pause");
    }
    return false;
}

void
Uart::write(const char *text, size_t length) {
    if (!enabled) {
        return;
    }
    for (size_t i = 0; i < length; i++) {
        if (text[i] == '\n' && waitForTransmitter()) {
            Ports::outb(COM1 + DATA, '\r');
        }
        if (!waitForTransmitter()) {
            return;
        }
        Ports::outb(COM1 + DATA, (uint8_t)text[i]);
    }
}
