#include "drivers/char/uart/Uart.h"
#include "kernel/Interrupts.h"
#include "kernel/Pic.h"
#include "lib/vitralKernel/LockGuard.h"
#include "lib/vitralKernel/Ports.h"

bool Uart::enabled = false;
bool Uart::found = false;
unsigned Uart::rounds = 0;
unsigned Uart::allowedRounds = 0;
SpinLock Uart::lock;
bool Uart::receiving = false;
char Uart::received[Uart::RECEIVE_SIZE];
unsigned Uart::receiveHead = 0;
unsigned Uart::receiveTail = 0;
SpinLock Uart::receiveLock;

// A 16450 or later keeps what is written to its scratch register, and in loopback mode returns each sent byte. An
// absent port reads 0xFF on an ISA bus and fails both.
bool
Uart::present() {
    Ports::outb(COM1 + SCRATCH, 0x5A);
    if (Ports::inb(COM1 + SCRATCH) != 0x5A) {
        return false;
    }
    Ports::outb(COM1 + SCRATCH, 0xA5);
    if (Ports::inb(COM1 + SCRATCH) != 0xA5) {
        return false;
    }
    Ports::outb(COM1 + MODEM_CONTROL, MODEM_LOOPBACK);
    while (Ports::inb(COM1 + LINE_STATUS) & DATA_READY) {
        Ports::inb(COM1 + DATA);
    }
    Ports::outb(COM1 + DATA, 0xAE);
    bool echoed = false;
    for (unsigned i = 0; i < WAIT_LIMIT && !echoed; i++) {
        echoed = Ports::inb(COM1 + LINE_STATUS) & DATA_READY;
    }
    bool matches = echoed && Ports::inb(COM1 + DATA) == 0xAE;
    Ports::outb(COM1 + MODEM_CONTROL, MODEM_NORMAL);
    return matches;
}

bool
Uart::initCom1() {
    Ports::outb(COM1 + INTERRUPT_ENABLE, 0);
    Ports::outb(COM1 + LINE_CONTROL, DLAB);
    Ports::outb(COM1 + DATA, (uint8_t)(DIVISOR & 0xFF));
    Ports::outb(COM1 + INTERRUPT_ENABLE, (uint8_t)(DIVISOR >> 8));
    Ports::outb(COM1 + LINE_CONTROL, 0x03);
    Ports::outb(COM1 + FIFO_CONTROL, 0xC7);
    found = present();
    enabled = found;
    return enabled;
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
Uart::send(const char *text, size_t length) {
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

void
Uart::write(const char *text, size_t length) {
    if (!enabled) {
        return;
    }
    LockGuard guard(lock);
    if (enabled) {
        send(text, length);
    }
}

void
Uart::writeUnlocked(const char *text, size_t length) {
    if (enabled) {
        send(text, length);
    }
}

// With receiveLock held. Bytes that do not fit are dropped.
void
Uart::drainReceiver() {
    for (unsigned i = 0; i < 64 && (Ports::inb(COM1 + LINE_STATUS) & DATA_READY); i++) {
        char c = (char)Ports::inb(COM1 + DATA);
        unsigned next = (receiveHead + 1) % RECEIVE_SIZE;
        if (next != receiveTail) {
            received[receiveHead] = c;
            receiveHead = next;
        }
    }
}

TrapFrame *
Uart::onIrq(TrapFrame *frame) {
    LockGuard guard(receiveLock);
    Ports::inb(COM1 + INTERRUPT_IDENTIFICATION);
    drainReceiver();
    return frame;
}

bool
Uart::enableReceive() {
    if (receiving) {
        return true;
    }
    if (!found) {
        return false;
    }
    {
        LockGuard guard(receiveLock);
        drainReceiver();
        receiveHead = receiveTail = 0;
    }
    if (Interrupts::addSharedIrqHandler(IRQ, onIrq)) {
        Ports::outb(COM1 + INTERRUPT_ENABLE, RECEIVE_INTERRUPT);
        Pic::unmask(IRQ);
    }
    receiving = true;
    return true;
}

size_t
Uart::read(char *buffer, size_t size) {
    if (!receiving) {
        return 0;
    }
    LockGuard guard(receiveLock);
    drainReceiver();
    size_t count = 0;
    while (count < size && receiveTail != receiveHead) {
        buffer[count++] = received[receiveTail];
        receiveTail = (receiveTail + 1) % RECEIVE_SIZE;
    }
    return count;
}

bool
Uart::readable() {
    if (!receiving) {
        return false;
    }
    LockGuard guard(receiveLock);
    drainReceiver();
    return receiveTail != receiveHead;
}
