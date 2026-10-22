#include <stddef.h>
#include "programs/vga/VgaProgram.h"
#include "kernel/ArgList.h"

int
vgaMain(int argc, char *argv[]);

int
VgaProgram::main(const ArgList &args) {
    char *argv[ArgList::MAX_ARGS + 2];
    int argc = 0;
    argv[argc++] = ((char *)("vga"));
    for (unsigned i = 0; i < args.count(); i++) {
        argv[argc++] = ((char *)(args.at(i)));
    }
    argv[argc] = NULL;
    return vgaMain(argc, argv);
}
