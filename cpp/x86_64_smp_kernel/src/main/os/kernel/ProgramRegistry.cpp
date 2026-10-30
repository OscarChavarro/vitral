#include "kernel/ProgramRegistry.h"
#include "kernel/Program.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "kernel/ArgList.h"

extern const Program programsStart[] asm("__programs_start");
extern const Program programsEnd[] asm("__programs_end");

unsigned
ProgramRegistry::count() { return (unsigned)(programsEnd - programsStart); }

const Program *
ProgramRegistry::at(unsigned i) { return i < count() ? &programsStart[i] : nullptr; }

const Program *
ProgramRegistry::find(const char *name) {
    for (unsigned i = 0; i < count(); i++) {
        if (String::equals(programsStart[i].name, name)) {
            return &programsStart[i];
        }
    }
    return nullptr;
}

unsigned
ProgramRegistry::menuCount() {
    unsigned n = 0;
    for (unsigned i = 0; i < count(); i++) {
        if (programsStart[i].inMenu) {
            n++;
        }
    }
    return n;
}

const Program *
ProgramRegistry::menuAt(unsigned i) {
    for (unsigned k = 0; k < count(); k++) {
        if (programsStart[k].inMenu && i-- == 0) {
            return &programsStart[k];
        }
    }
    return nullptr;
}
