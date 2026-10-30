#include "kernel/Process.h"
#include "kernel/Gdt.h"
#include "lib/vitralKernel/Memory.h"

// A ring 0 iret pops only eip, cs and eflags: start() then runs on the stack right above the frame, where a null
// return address is left (its entry stack is aligned as if it had been called).
void
Process::prepareFrame() {
    char *top = stack + stackSize - 16;
    *(uint32_t *)(top - 4) = 0;
    TrapFrame *f = (TrapFrame *)(top - 4 - sizeof(TrapFrame));
    Memory::set(f, 0, sizeof(TrapFrame));
    f->eip = (uint32_t)(uintptr_t)&Process::start;
    f->cs = Gdt::CODE;
    f->eflags = 0x202;
    frame = f;
}
