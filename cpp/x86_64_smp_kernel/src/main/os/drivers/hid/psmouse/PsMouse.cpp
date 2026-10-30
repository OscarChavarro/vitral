#include "drivers/hid/psmouse/PsMouse.h"
#include "drivers/hid/MouseDevice.h"
#include "kernel/Interrupts.h"
#include "kernel/KernelLog.h"
#include "kernel/Pic.h"
#include "lib/vitralKernel/Ports.h"

bool PsMouse::found = false;
uint8_t PsMouse::id = 0;
uint8_t PsMouse::packet[4];
unsigned PsMouse::received = 0;

static constexpr unsigned IRQ = 12;
static constexpr unsigned TRIES = 100000;

bool
PsMouse::waitInput() {
    for (unsigned i = 0; i < TRIES; i++) {
        if (!(Ports::inb(STATUS) & STATUS_INPUT_FULL)) {
            return true;
        }
    }
    return false;
}

bool
PsMouse::waitOutput() {
    for (unsigned i = 0; i < TRIES; i++) {
        if (Ports::inb(STATUS) & STATUS_OUTPUT_FULL) {
            return true;
        }
    }
    return false;
}

bool
PsMouse::readAux(uint8_t &byte) {
    for (unsigned i = 0; i < 16; i++) {
        if (!waitOutput()) {
            return false;
        }
        bool aux = Ports::inb(STATUS) & STATUS_AUX;
        byte = Ports::inb(DATA);
        if (aux) {
            return true;
        }
    }
    return false;
}

bool
PsMouse::sendCommand(uint8_t command) {
    for (unsigned attempt = 0; attempt < 2; attempt++) {
        if (!waitInput()) {
            return false;
        }
        Ports::outb(COMMAND, 0xD4);
        if (!waitInput()) {
            return false;
        }
        Ports::outb(DATA, command);
        uint8_t answer;
        if (!readAux(answer)) {
            return false;
        }
        if (answer == ACK) {
            return true;
        }
        if (answer != 0xFE) {
            return false;
        }
    }
    return false;
}

bool
PsMouse::setSampleRate(uint8_t rate) { return sendCommand(0xF3) && sendCommand(rate); }

int
PsMouse::readId() {
    uint8_t value;
    if (!sendCommand(0xF2) || !readAux(value)) {
        return -1;
    }
    return value;
}

void
PsMouse::disable() {
    if (waitInput()) {
        Ports::outb(COMMAND, 0xA7);
    }
}

const char *
PsMouse::model() {
    return id == 4 ? "IntelliMouse Explorer" : id == 3 ? "IntelliMouse"
                                                       : "PS/2";
}

bool
PsMouse::init() {
    if (!waitInput()) {
        return false;
    }
    Ports::outb(COMMAND, 0xA8);
    if (!waitInput()) {
        return false;
    }
    Ports::outb(COMMAND, 0x20);
    if (!waitOutput()) {
        return false;
    }
    uint8_t config = Ports::inb(DATA);
    config = (uint8_t)((config | 0x02) & ~0x20);
    if (!waitInput()) {
        return false;
    }
    Ports::outb(COMMAND, 0x60);
    if (!waitInput()) {
        return false;
    }
    Ports::outb(DATA, config);

    if (!sendCommand(0xF6)) {
        disable();
        kprintf("PS/2 mouse: not found\n");
        return false;
    }
    id = 0;
    if (setSampleRate(200) && setSampleRate(100) && setSampleRate(80) && readId() == 3) {
        id = 3;
        if (setSampleRate(200) && setSampleRate(200) && setSampleRate(80) && readId() == 4) {
            id = 4;
        }
    }
    setSampleRate(100);
    received = 0;
    if (!Interrupts::registerHandler(Interrupts::IRQ_BASE + IRQ, onIrq)) {
        disable();
        kprintf("PS/2 mouse: IRQ %u already taken\n", IRQ);
        return false;
    }
    if (!sendCommand(0xF4)) {
        Interrupts::unregisterHandler(Interrupts::IRQ_BASE + IRQ);
        disable();
        kprintf("PS/2 mouse: does not stream\n");
        return false;
    }
    Pic::unmask(IRQ);
    found = true;
    kprintf("PS/2 mouse: %s, %u byte packets, IRQ %u, /dev/input/mice\n", model(), id ? 4u : 3u, IRQ);
    return true;
}

TrapFrame *
PsMouse::onIrq(TrapFrame *frame) {
    uint8_t status = Ports::inb(STATUS);
    if ((status & (STATUS_OUTPUT_FULL | STATUS_AUX)) != (STATUS_OUTPUT_FULL | STATUS_AUX)) {
        return frame;
    }
    uint8_t byte = Ports::inb(DATA);
    if (received == 0 && !(byte & 0x08)) {
        return frame;
    }
    packet[received++] = byte;
    if (received < (id ? 4u : 3u)) {
        return frame;
    }
    received = 0;

    uint8_t flags = packet[0];
    int dx = packet[1] - ((flags << 4) & 0x100);
    int dy = packet[2] - ((flags << 3) & 0x100);
    if (flags & 0xC0) {
        dx = dy = 0;
    }
    uint8_t buttons = flags & (MouseDevice::LEFT | MouseDevice::RIGHT | MouseDevice::MIDDLE);
    int wheel = 0;
    if (id == 3) {
        wheel = (int8_t)packet[3];
    } else if (id == 4) {
        wheel = (packet[3] & 0x08) ? (packet[3] & 0x0F) - 16 : packet[3] & 0x0F;
        if (packet[3] & 0x10) {
            buttons |= MouseDevice::FOURTH;
        }
        if (packet[3] & 0x20) {
            buttons |= MouseDevice::FIFTH;
        }
    }
    MouseDevice::report(dx, -dy, wheel, buttons);
    return frame;
}
