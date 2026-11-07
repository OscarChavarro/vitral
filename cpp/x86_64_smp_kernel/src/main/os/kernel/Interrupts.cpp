#include "kernel/Interrupts.h"
#include "kernel/Gdt.h"
#include "kernel/Panic.h"
#include "kernel/Pic.h"

extern const char stubBase[] asm("interrupt_stubs");

IdtGate Interrupts::idt[Interrupts::VECTORS];
Interrupts::Handler Interrupts::handlers[Interrupts::VECTORS];
Interrupts::Handler Interrupts::sharedHandlers[Interrupts::PIC_LINES][Interrupts::SHARED_PER_LINE];

void
Interrupts::init() {
    for (unsigned v = 0; v < VECTORS; v++) {
        idt[v] = IdtGate::interrupt((uintptr_t)stubBase + v * STUB_SIZE, Gdt::CODE);
        handlers[v] = nullptr;
    }
    for (unsigned line = 0; line < PIC_LINES; line++) {
        for (unsigned i = 0; i < SHARED_PER_LINE; i++) {
            sharedHandlers[line][i] = nullptr;
        }
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

bool
Interrupts::addSharedIrqHandler(unsigned irq, Handler handler) {
    if (irq >= PIC_LINES) {
        return false;
    }
    for (unsigned i = 0; i < SHARED_PER_LINE; i++) {
        if (sharedHandlers[irq][i] == handler) {
            return true;
        }
        if (!sharedHandlers[irq][i]) {
            sharedHandlers[irq][i] = handler;
            return true;
        }
    }
    return false;
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
    if (vector >= IRQ_BASE && vector < IRQ_BASE + PIC_LINES) {
        for (Handler shared : sharedHandlers[vector - IRQ_BASE]) {
            if (shared) {
                resume = shared(resume);
            }
        }
        Pic::eoi(vector - IRQ_BASE);
    }
    return resume;
}
