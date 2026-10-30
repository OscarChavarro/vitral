#include "kernel/Interrupts.h"
#include "kernel/Gdt.h"
#include "kernel/Panic.h"
#include "kernel/Pic.h"

extern const char stubBase[] asm("interrupt_stubs");

IdtGate Interrupts::idt[Interrupts::VECTORS];
Interrupts::Handler Interrupts::handlers[Interrupts::VECTORS];

void
Interrupts::init() {
    for (unsigned v = 0; v < VECTORS; v++) {
        idt[v] = IdtGate::interrupt((uintptr_t)stubBase + v * STUB_SIZE, Gdt::CODE);
        handlers[v] = nullptr;
    }
    load();
}

void
Interrupts::load() {
    Pointer p = {sizeof(idt) - 1, (uintptr_t)idt};
    asm volatile("lidt %0" : : "m"(p));
}

bool
Interrupts::registerHandler(unsigned vector, Handler handler) {
    if (vector >= VECTORS || handlers[vector]) {
        return false;
    }
    handlers[vector] = handler;
    return true;
}

void
Interrupts::unregisterHandler(unsigned vector) {
    if (vector < VECTORS) {
        handlers[vector] = nullptr;
    }
}

TrapFrame *
Interrupts::dispatch(TrapFrame *frame) {
    unsigned vector = (unsigned)frame->vector;
    Handler handler = handlers[vector];
    TrapFrame *resume = frame;
    if (handler) {
        resume = handler(frame);
    } else if (vector < EXCEPTIONS) {
        Panic::exception(*frame);
    }
    if (vector >= IRQ_BASE && vector < IRQ_BASE + 16) {
        Pic::eoi(vector - IRQ_BASE);
    }
    return resume;
}
