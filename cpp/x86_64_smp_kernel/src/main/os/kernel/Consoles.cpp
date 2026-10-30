#include "kernel/Consoles.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/LockGuard.h"

uint32_t Consoles::owner[Consoles::COUNT];
bool Consoles::retained[Consoles::COUNT];
SpinLock Consoles::lock;

int
Consoles::allocate(uint32_t pid) {
    LockGuard guard(lock);
    for (unsigned c = 0; c < COUNT; c++) {
        if (owner[c] == 0) {
            owner[c] = pid;
            retained[c] = false;
            return (int)c;
        }
    }
    return -1;
}

void
Consoles::showEmptyMessage(unsigned console) {
    char line[VirtualConsole::COLS + 1];
    VirtualConsole::clear(console, 0x07);
    Format::print(line, sizeof(line), "KERNEL: Virtual console %u is empty", console);
    VirtualConsole::writeAt(console, 0, 0, line, 0x07);
    VirtualConsole::setCursor(console, 0, 1);
}

void
Consoles::release(unsigned console) {
    if (console >= COUNT) {
        return;
    }
    LockGuard guard(lock);
    owner[console] = 0;
    retained[console] = false;
    showEmptyMessage(console);
}

void
Consoles::retire(unsigned console, const char *name, int exitCode) {
    if (console >= COUNT) {
        return;
    }
    {
        LockGuard guard(lock);
        owner[console] = 0;
        retained[console] = true;
    }
    char line[VirtualConsole::COLS + 1];
    Format::print(line, sizeof(line), "[%s: exited with status %d]\n", name, exitCode);
    if (VirtualConsole::cursorX(console) != 0) {
        VirtualConsole::putc(console, '\n');
    }
    VirtualConsole::setAttr(console, 0x07);
    VirtualConsole::showCursor(console, true);
    for (const char *c = line; *c; c++) {
        VirtualConsole::putc(console, *c);
    }
}

bool
Consoles::isEmpty(unsigned console) {
    LockGuard guard(lock);
    return console < COUNT && owner[console] == 0;
}

void
Consoles::switchTo(unsigned console) {
    if (console >= COUNT) {
        return;
    }
    {
        LockGuard guard(lock);
        if (owner[console] == 0 && !retained[console]) {
            showEmptyMessage(console);
        }
    }
    VirtualConsole::switchTo(console);
}
