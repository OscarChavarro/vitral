#include "kernel/Panic.h"
#include "lib/vitralKernel/Printf.h"

void
Panic::printRegisters(const TrapFrame &f) {
    printf64("  EIP=%08x ESP=%08x EFLAGS=%08x\n", f.eip, (uint32_t)f.stackPointer(), f.eflags);
    printf64("  EAX=%08x EBX=%08x ECX=%08x EDX=%08x\n", f.eax, f.ebx, f.ecx, f.edx);
    printf64("  ESI=%08x EDI=%08x EBP=%08x\n", f.esi, f.edi, f.ebp);
}
