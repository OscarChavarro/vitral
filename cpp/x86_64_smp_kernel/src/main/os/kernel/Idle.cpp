#include "kernel/Idle.h"
#include "lib/vitralKernel/Cpu.h"
#include "lib/vitralKernel/Printf.h"

Idle::Mode
Idle::mode() {
    if (Cpu::hasMWait()) {
        return Mode::MWait;
    }
    if (Cpu::hasHlt()) {
        return Mode::Hlt;
    }
    return Mode::Nop;
}

bool
Idle::canSleep() { return mode() != Mode::Nop; }

const char *
Idle::name() {
    switch (mode()) {
    case Mode::MWait: return "MWAIT";
    case Mode::Hlt: return "HLT";
    default: return "a NOP loop";
    }
}

void
Idle::loop(Mode m, bool interruptsOn) {
    static volatile char line[64] __attribute__((aligned(64)));
    for (;;) {
        if (interruptsOn) {
            asm volatile("sti");
        } else {
            asm volatile("cli");
        }
        switch (m) {
        case Mode::MWait:
            asm volatile("monitor" : : "a"(line), "c"(0), "d"(0));
            asm volatile("mwait" : : "a"(0), "c"(0));
            break;
        case Mode::Hlt:
            asm volatile("hlt");
            break;
        case Mode::Nop:
            asm volatile("nop");
            break;
        }
    }
}

void
Idle::run() { loop(mode(), true); }

void
Idle::stop() {
    printf64("Kernel halted using %s.\n", name());
    loop(mode(), false);
}
