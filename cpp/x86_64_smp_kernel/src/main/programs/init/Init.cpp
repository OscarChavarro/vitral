#include "programs/init/Init.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "kernel/InitScripts.h"
#include "kernel/KernelLog.h"
#include "syscalls/Syscalls.h"
#include "kernel/ArgList.h"

int
Init::main(const ArgList &) {
    unsigned failures = InitScripts::run();
    if (failures) {
        kprintf("init: %u start-up step(s) failed\n", failures);
        Syscalls::sleep(FAILURE_PAUSE_MS);
    }
    unsigned console = Syscalls::console();
    ArgList none;
    for (;;) {
        VirtualConsole::clear(console, 0x07);
        int64_t top = Syscalls::run("top", none);
        if (top >= 0) {
            int status;
            Syscalls::waitpid(top, &status);
        }
        Syscalls::sleep(RESTART_PAUSE_MS);
    }
}
