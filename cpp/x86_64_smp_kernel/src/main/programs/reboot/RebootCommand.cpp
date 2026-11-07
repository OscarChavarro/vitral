#include "programs/reboot/RebootCommand.h"
#include "lib/vitralKernel/Printf.h"
#include "syscalls/Syscalls.h"

int
RebootCommand::main(const ArgList &args) {
    if (args.count() != 0) {
        printf64("usage: reboot\n");
        return 1;
    }
    printf64("Rebooting...\n");
    Syscalls::sleep(FLUSH_MS);
    Syscalls::reboot();
}
