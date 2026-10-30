#ifndef __KERNEL_X86_64_TRAP_FRAME__
#define __KERNEL_X86_64_TRAP_FRAME__
#include <stdint.h>

// Registers saved by kernel/x86_64/interrupt_stubs.S, in the order they sit on the stack. The CPU pushes ss and rsp
// on every interrupt in long mode.
class TrapFrame {
public:
    uint64_t r15;
    uint64_t r14;
    uint64_t r13;
    uint64_t r12;
    uint64_t r11;
    uint64_t r10;
    uint64_t r9;
    uint64_t r8;
    uint64_t rbp;
    uint64_t rdi;
    uint64_t rsi;
    uint64_t rdx;
    uint64_t rcx;
    uint64_t rbx;
    uint64_t rax;
    uint64_t vector;
    uint64_t errorCode;
    uint64_t rip;
    uint64_t cs;
    uint64_t rflags;
    uint64_t rsp;
    uint64_t ss;

    // System call ABI (Syscalls::call): number in rax, arguments in rdi, rsi, rdx and r10, result in rax.
    uint64_t number() const { return rax; }
    uint64_t argument(unsigned index) const {
        return index == 0 ? rdi : index == 1 ? rsi : index == 2 ? rdx : r10;
    }
    void setResult(uint64_t value) { rax = value; }
    uintptr_t instructionPointer() const { return rip; }
    uintptr_t stackPointer() const { return rsp; }
};

#endif
