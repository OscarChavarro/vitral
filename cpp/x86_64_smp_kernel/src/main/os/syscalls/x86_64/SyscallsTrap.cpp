#include "syscalls/Syscalls.h"

// Long mode system call: number in rax, arguments in rdi, rsi, rdx and r10, result in rax (kernel/x86_64/TrapFrame.h).
int64_t
Syscalls::call(uint64_t number, uint64_t a, uint64_t b, uint64_t c, uint64_t d) {
    int64_t result;
    register uint64_t r10 asm("r10") = d;
    asm volatile("int $0x80" : "=a"(result) : "a"(number), "D"(a), "S"(b), "d"(c), "r"(r10) : "memory");
    return result;
}
