#include "drivers/char/uart/Uart.h"
#include "lib/vitralKernel/LockGuard.h"
#include "lib/vitralKernel/Ports.h"

bool Uart::enabled = false;
unsigned Uart::rounds = 0;
unsigned Uart::allowedRounds = 0;
SpinLock Uart::lock;

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

// Gates channel 2 on with the speaker off and loads its count, which restarts it.
void
Uart::startRound() {
    uint8_t control = Ports::inb(SYSTEM_CONTROL);
    Ports::outb(SYSTEM_CONTROL, (uint8_t)((control & ~SPEAKER_DATA) | CHANNEL2_GATE));
    Ports::outb(PIT_COMMAND, CHANNEL2_MODE0);
    Ports::outb(PIT_CHANNEL2, 0xFF);
    Ports::outb(PIT_CHANNEL2, 0xFF);
}

// False when the transmitter stays busy for WAIT_LIMIT polls or the write has used up its rounds.
bool
Uart::waitForTransmitter() {
    for (unsigned i = 0; i < WAIT_LIMIT; i++) {
        if (Ports::inb(COM1 + LINE_STATUS) & TRANSMITTER_EMPTY) {
            return true;
        }
        if (Ports::inb(SYSTEM_CONTROL) & CHANNEL2_OUTPUT) {
            if (++rounds > allowedRounds) {
                return false;
            }
            startRound();
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
    LockGuard guard(lock);
    if (!enabled) {
        return;
    }
    rounds = 0;
    allowedRounds = SPARE_ROUNDS + (unsigned)(length / CHARACTERS_PER_ROUND);
    startRound();
    for (size_t i = 0; i < length; i++) {
        bool sent = text[i] != '\n' || waitForTransmitter();
        if (sent && text[i] == '\n') {
            Ports::outb(COM1 + DATA, '\r');
        }
        if (!sent || !waitForTransmitter()) {
            enabled = false;
            return;
        }
        Ports::outb(COM1 + DATA, (uint8_t)text[i]);
    }
}
