#include <stddef.h>
#include "programs/vitralRasterizer/VitralRasterizerProgram.h"
#include "kernel/ArgList.h"

int
vitralRasterizerMain(int argc, char *argv[]);

int
VitralRasterizerProgram::main(const ArgList &args) {
    char *argv[ArgList::MAX_ARGS + 2];
    int argc = 0;
    argv[argc++] = ((char *)("vitral_rasterizer"));
    for (unsigned i = 0; i < args.count(); i++) {
        argv[argc++] = ((char *)(args.at(i)));
    }
    argv[argc] = NULL;
    return vitralRasterizerMain(argc, argv);
}
