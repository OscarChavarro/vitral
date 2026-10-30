#include "boot/A20.h"
#include "lib/vitralKernel/Ports.h"

bool
A20::isEnabled() {
    uint8_t probe;
    asm volatile("pushw %%fs\n\t"
                 "movw $0xFFFF, %%ax\n\t"
                 "movw %%ax, %%fs\n\t"
                 "movb 0x500, %%cl\n\t"
                 "movb %%fs:0x510, %%ch\n\t"
                 "movb $0x00, 0x500\n\t"
                 "movb $0xFF, %%fs:0x510\n\t"
                 "movb 0x500, %%al\n\t"
                 "movb %%cl, 0x500\n\t"
                 "movb %%ch, %%fs:0x510\n\t"
                 "popw %%fs"
                 : "=&a"(probe) : : "cx", "memory");
    return probe != 0xFF;
}

bool
A20::settled() {
    for (int i = 0; i < 2000; i++) {
        if (isEnabled()) {
            return true;
        }
    }
    return false;
}

bool
A20::tryBios() {
    uint16_t ax = 0x2401;
    bool carry;
    asm volatile("int $0x15" : "+a"(ax), "=@ccc"(carry) : : "memory");
    return !carry && (ax >> 8) == 0;
}

void
A20::tryFast() {
    uint8_t v = Ports::inb(0x92);
    if (v == 0xFF) {
        return;
    }
    if (!(v & 2)) {
        Ports::outb(0x92, (uint8_t)((v | 2) & ~1));
    }
}

bool
A20::waitInputEmpty() {
    for (long i = 0; i < 100000; i++) {
        if (!(Ports::inb(0x64) & 2)) {
            return true;
        }
    }
    return false;
}

bool
A20::waitOutputFull() {
    for (long i = 0; i < 100000; i++) {
        if (Ports::inb(0x64) & 1) {
            return true;
        }
    }
    return false;
}

void
A20::tryKeyboardController() {
    if (!waitInputEmpty()) {
        return;
    }
    Ports::outb(0x64, 0xAD);
    if (!waitInputEmpty()) {
        return;
    }
    Ports::outb(0x64, 0xD0);
    if (!waitOutputFull()) {
        return;
    }
    uint8_t port = Ports::inb(0x60);
    if (!waitInputEmpty()) {
        return;
    }
    Ports::outb(0x64, 0xD1);
    if (!waitInputEmpty()) {
        return;
    }
    Ports::outb(0x60, port | 2);
    waitInputEmpty();
    Ports::outb(0x64, 0xAE);
    waitInputEmpty();
}

bool
A20::enable() {
    if (isEnabled()) {
        return true;
    }
    tryBios();
    if (settled()) {
        return true;
    }
    tryFast();
    if (settled()) {
        return true;
    }
    tryKeyboardController();
    return settled();
}
