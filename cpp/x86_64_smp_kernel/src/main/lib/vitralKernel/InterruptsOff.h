#ifndef __INTERRUPTS_OFF__
#define __INTERRUPTS_OFF__
#include <stdint.h>

// No interrupt reaches this CPU while it lives, so neither does a move to another CPU: what a system call handler gets
// from its interrupt gate, for the kernel functions programs call directly. Nests.
class InterruptsOff {
public:
    InterruptsOff() { asm volatile("pushf; pop %0; cli" : "=r"(flags) : : "memory"); }
    ~InterruptsOff() {
        if (flags & 0x200) {
            asm volatile("sti" : : : "memory");
        }
    }
    InterruptsOff(const InterruptsOff &) = delete;
    InterruptsOff &operator=(const InterruptsOff &) = delete;

private:
    uintptr_t flags;
};

#endif
