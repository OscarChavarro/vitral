#include "syscalls/Syscalls.h"

// 32-bit system call: the arguments are 64-bit values (offsets, sizes, nanoseconds), so they travel in memory:
// number in eax, address of the four arguments in ebx, result in edx:eax (kernel/i386/TrapFrame.h).
int64_t
Syscalls::call(uint64_t number, uint64_t a, uint64_t b, uint64_t c, uint64_t d) {
    uint64_t arguments[4] = {a, b, c, d};
    uint32_t low;
    uint32_t high;
    asm volatile("int $0x80" : "=a"(low), "=d"(high) : "a"((uint32_t)number), "b"(arguments) : "memory");
    return (int64_t)(((uint64_t)high << 32) | low);
}
