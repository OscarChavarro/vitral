#include "kernel/Panic.h"

void
Panic::printRegisters(const TrapFrame &f) {
    auto hi = [](uint64_t v) { return (uint32_t)(v >> 32); };
    auto lo = [](uint64_t v) { return (uint32_t)v; };
    print("  RIP=%08x%08x RSP=%08x%08x RFLAGS=%08x\n", hi(f.rip), lo(f.rip), hi(f.rsp), lo(f.rsp), lo(f.rflags));
    print("  RAX=%08x%08x RBX=%08x%08x RCX=%08x%08x\n", hi(f.rax), lo(f.rax), hi(f.rbx), lo(f.rbx), hi(f.rcx),
          lo(f.rcx));
    print("  RDX=%08x%08x RSI=%08x%08x RDI=%08x%08x\n", hi(f.rdx), lo(f.rdx), hi(f.rsi), lo(f.rsi), hi(f.rdi),
          lo(f.rdi));
}
