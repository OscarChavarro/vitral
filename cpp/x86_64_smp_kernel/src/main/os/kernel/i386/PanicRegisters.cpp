#include "kernel/Panic.h"

void
Panic::printRegisters(const TrapFrame &f) {
    print("  EIP=%08x ESP=%08x EFLAGS=%08x\n", f.eip, (uint32_t)f.stackPointer(), f.eflags);
    print("  EAX=%08x EBX=%08x ECX=%08x EDX=%08x\n", f.eax, f.ebx, f.ecx, f.edx);
    print("  ESI=%08x EDI=%08x EBP=%08x\n", f.esi, f.edi, f.ebp);
}
