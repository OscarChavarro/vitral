#ifndef __INTERRUPTS__
#define __INTERRUPTS__
#include <stdint.h>
#include "kernel/IdtGate.h"
#include "kernel/TrapFrame.h"

class Interrupts {
public:
    using Handler = TrapFrame *(*)(TrapFrame *);

    static constexpr unsigned VECTORS = 256;
    static constexpr unsigned EXCEPTIONS = 32;
    static constexpr unsigned IRQ_BASE = 0x20;
    static constexpr unsigned SYSCALL = 0x80;
    static constexpr unsigned LAPIC_TIMER = 0x40;
    static constexpr unsigned RESCHEDULE = 0x41;
    static constexpr unsigned SPURIOUS = 0xFF;

    static void init();
    static void load();
    static bool registerHandler(unsigned vector, Handler handler);
    static void unregisterHandler(unsigned vector);
    // PCI devices may share a PIC line with each other and with the device that registerHandler() gave the vector to.
    // Each shared handler checks its own device and returns the frame unchanged; all of them run on every interrupt
    // of the line, after the registered handler.
    static bool addSharedIrqHandler(unsigned irq, Handler handler);

    static void enable() { asm volatile("sti" : : : "memory"); }
    static void disable() { asm volatile("cli" : : : "memory"); }

    static TrapFrame *dispatch(TrapFrame *frame);

private:
    // Size of each entry stub in interrupt_stubs.S (both CPU modes).
    static constexpr unsigned STUB_SIZE = 16;

    class Pointer {
    public:
        uint16_t limit;
        uintptr_t base;
    } __attribute__((packed));

    static constexpr unsigned PIC_LINES = 16;
    static constexpr unsigned SHARED_PER_LINE = 4;

    static IdtGate idt[VECTORS];
    static Handler handlers[VECTORS];
    static Handler sharedHandlers[PIC_LINES][SHARED_PER_LINE];
};

#endif
