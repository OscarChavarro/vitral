#include "kernel/Kernel.h"
#include "kernel/Panic.h"
#include "kernel/Program.h"
#include "kernel/Scheduler.h"
#include "kernel/ArgList.h"
#include "kernel/ProgramRegistry.h"

void
Kernel::startInit(const char *name) {
    const Program *program = ProgramRegistry::find(name);
    if (!program) {
        Panic::halt("the init program is not registered");
    }
    ArgList args;
    Process *init = Scheduler::spawn(*program, args, 0, CpuMask::only(0));
    if (!init || init->pid() != 1) {
        Panic::halt("cannot create init (PID 1)");
    }
}
