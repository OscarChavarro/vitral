#ifndef __PROGRAM_REGISTRY__
#define __PROGRAM_REGISTRY__
#include "kernel/Program.h"

class ProgramRegistry {
public:
    static const Program *find(const char *name);
    static unsigned count();
    static const Program *at(unsigned i);
    static unsigned menuCount();
    static const Program *menuAt(unsigned i);
};

#endif
