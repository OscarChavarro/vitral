#ifndef __KERNEL_X86_64_IDT_GATE__
#define __KERNEL_X86_64_IDT_GATE__
#include <stdint.h>

// A 16-byte long mode interrupt gate.
class IdtGate {
public:
    uint16_t offsetLow;
    uint16_t selector;
    uint8_t ist;
    uint8_t typeAttr;
    uint16_t offsetMid;
    uint32_t offsetHigh;
    uint32_t zero;

    static IdtGate interrupt(uintptr_t entry, uint16_t selector) {
        return {(uint16_t)entry, selector, 0, 0x8E, (uint16_t)(entry >> 16), (uint32_t)((uint64_t)entry >> 32), 0};
    }
} __attribute__((packed));

#endif
