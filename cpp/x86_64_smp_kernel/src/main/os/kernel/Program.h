#ifndef __PROGRAM__
#define __PROGRAM__
#include <stdint.h>
#include <stddef.h>
#include "kernel/ArgList.h"

class Program {
public:
    const char *name;
    int (*main)(const ArgList &args);
    bool inMenu = true;
};

#endif
