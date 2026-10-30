#ifndef __KERNEL_I386_IDT_GATE__
#define __KERNEL_I386_IDT_GATE__
#include <stdint.h>

// An 8-byte 32-bit protected mode interrupt gate.
class IdtGate {
public:
    uint16_t offsetLow;
    uint16_t selector;
    uint8_t zero;
    uint8_t typeAttr;
    uint16_t offsetHigh;

    static IdtGate interrupt(uintptr_t entry, uint16_t selector) {
        return {(uint16_t)entry, selector, 0, 0x8E, (uint16_t)(entry >> 16)};
    }
} __attribute__((packed));

#endif
