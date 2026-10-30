#ifndef __KERNEL_I386_TRAP_FRAME__
#define __KERNEL_I386_TRAP_FRAME__
#include <stdint.h>

// Registers saved by kernel/i386/interrupt_stubs.S (pushal order), in the order they sit on the stack. An interrupt
// taken in ring 0 does not push ss and esp: the frame ends at eflags and the interrupted code's stack follows it.
class TrapFrame {
public:
    uint32_t edi;
    uint32_t esi;
    uint32_t ebp;
    uint32_t espAtPush;
    uint32_t ebx;
    uint32_t edx;
    uint32_t ecx;
    uint32_t eax;
    uint32_t vector;
    uint32_t errorCode;
    uint32_t eip;
    uint32_t cs;
    uint32_t eflags;

    // System call ABI (Syscalls::call): number in eax and, in ebx, the address of the four 64-bit arguments; the
    // 64-bit result goes back in edx:eax.
    uint64_t number() const { return eax; }
    uint64_t argument(unsigned index) const { return ((const uint64_t *)(uintptr_t)ebx)[index]; }
    void setResult(uint64_t value) {
        eax = (uint32_t)value;
        edx = (uint32_t)(value >> 32);
    }
    uintptr_t instructionPointer() const { return eip; }
    uintptr_t stackPointer() const { return (uintptr_t)(this + 1); }
};

#endif
