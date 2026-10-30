#include "kernel/Process.h"
#include "kernel/Gdt.h"
#include "lib/vitralKernel/Memory.h"

void
Process::prepareFrame() {
    TrapFrame *f = (TrapFrame *)(stack + stackSize - sizeof(TrapFrame));
    Memory::set(f, 0, sizeof(TrapFrame));
    f->rip = (uint64_t)(uintptr_t)&Process::start;
    f->cs = Gdt::CODE;
    f->ss = Gdt::DATA;
    f->rflags = 0x202;
    f->rsp = (uint64_t)(uintptr_t)f - 8;
    *(uint64_t *)f->rsp = 0;
    frame = f;
}
