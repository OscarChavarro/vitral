#include "drivers/keyboard/Keyboard.h"
#include "drivers/hid/psmouse/PsMouse.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "kernel/Consoles.h"
#include "kernel/Interrupts.h"
#include "kernel/Panic.h"
#include "kernel/Pic.h"
#include "kernel/Scheduler.h"
#include "lib/vitralKernel/Ports.h"

volatile uint16_t Keyboard::queue[12][Keyboard::SIZE];
volatile unsigned Keyboard::head[12];
volatile unsigned Keyboard::tail[12];
bool Keyboard::shift = false;
bool Keyboard::alt = false;
bool Keyboard::ctrl = false;
bool Keyboard::extended = false;

static const char PLAIN[0x3A] = {
    0, 27, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', '\b', '\t',
    'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', '\n', 0, 'a', 's',
    'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'', '`', 0, '\\', 'z', 'x', 'c', 'v',
    'b', 'n', 'm', ',', '.', '/', 0, '*', 0, ' ',
};
static const char SHIFTED[0x3A] = {
    0, 27, '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', '\b', '\t',
    'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', '\n', 0, 'A', 'S',
    'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '"', '~', 0, '|', 'Z', 'X', 'C', 'V',
    'B', 'N', 'M', '<', '>', '?', 0, '*', 0, ' ',
};

void
Keyboard::init() {
    for (unsigned c = 0; c < 12; c++) {
        head[c] = tail[c] = 0;
    }
    while (Ports::inb(0x64) & 1) {
        Ports::inb(0x60);
    }
    if (!Interrupts::registerHandler(Interrupts::IRQ_BASE + 1, onIrq)) {
        Panic::halt("keyboard vector already taken");
    }
    Pic::unmask(1);
}

void
Keyboard::push(unsigned console, uint16_t key) {
    unsigned next = (head[console] + 1) & (SIZE - 1);
    if (next == tail[console]) {
        return;
    }
    queue[console][head[console]] = key;
    head[console] = next;
}

int
Keyboard::read() {
    unsigned console = Scheduler::currentConsole();
    if (tail[console] == head[console]) {
        return -1;
    }
    uint16_t key = queue[console][tail[console]];
    tail[console] = (tail[console] + 1) & (SIZE - 1);
    return key;
}

bool
Keyboard::pending() {
    unsigned console = Scheduler::currentConsole();
    return tail[console] != head[console];
}

uint8_t
Keyboard::modifiers() {
    return (uint8_t)((shift ? MOD_SHIFT : 0) | (ctrl ? MOD_CTRL : 0) | (alt ? MOD_ALT : 0));
}

TrapFrame *
Keyboard::onIrq(TrapFrame *frame) {
    uint8_t status = Ports::inb(0x64);
    if (!(status & 0x01)) {
        return frame;
    }
    if (status & 0x20) {
        if (!PsMouse::present()) {
            Ports::inb(0x60);
        }
        return frame;
    }
    uint8_t sc = Ports::inb(0x60);
    if (sc == 0xE0) { extended = true; return frame; }
    bool isExtended = extended;
    extended = false;
    bool release = sc & 0x80;
    sc &= 0x7F;

    if (sc == 0x38) {
        alt = !release;
        return frame;
    }
    if (sc == 0x1D) {
        ctrl = !release;
        return frame;
    }
    if (sc == 0x2A || sc == 0x36) {
        shift = !release;
        return frame;
    }
    if (release) {
        return frame;
    }

    int function = -1;
    if (sc >= 0x3B && sc <= 0x44) {
        function = sc - 0x3B;
    } else if (sc == 0x57) {
        function = 10;
    } else if (sc == 0x58) {
        function = 11;
    }
    if (alt && function >= 0) {
        Consoles::switchTo((unsigned)function);
        return frame;
    }

    unsigned console = VirtualConsole::active();
    if (function >= 0) {
        push(console, (uint16_t)(KEY_F1 + function));
        return frame;
    }
    if (isExtended) {
        switch (sc) {
        case 0x48: push(console, KEY_UP); break;
        case 0x50: push(console, KEY_DOWN); break;
        case 0x4B: push(console, KEY_LEFT); break;
        case 0x4D: push(console, KEY_RIGHT); break;
        case 0x49: push(console, KEY_PAGE_UP); break;
        case 0x51: push(console, KEY_PAGE_DOWN); break;
        }
    } else if (sc < sizeof(PLAIN)) {
        char c = shift ? SHIFTED[sc] : PLAIN[sc];
        if (ctrl) {
            if (c >= 'a' && c <= 'z') {
                push(console, (uint8_t)(c - 'a' + 1));
            }
        } else if (c) {
            push(console, (uint8_t)c);
        }
    }
    return frame;
}
