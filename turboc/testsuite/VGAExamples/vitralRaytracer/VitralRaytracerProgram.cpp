#include <stddef.h>
#include "programs/vitralRaytracer/VitralRaytracerProgram.h"
#include "kernel/ArgList.h"

int
vitralRaytracerMain(int argc, char *argv[]);

int
VitralRaytracerProgram::main(const ArgList &args) {
    char *argv[ArgList::MAX_ARGS + 2];
    int argc = 0;
    argv[argc++] = ((char *)("vitral_raytracer"));
    for (unsigned i = 0; i < args.count(); i++) {
        argv[argc++] = ((char *)(args.at(i)));
    }
    argv[argc] = NULL;
    return vitralRaytracerMain(argc, argv);
}
