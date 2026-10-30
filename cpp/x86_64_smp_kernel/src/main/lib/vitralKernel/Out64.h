#ifndef __OUT64__
#define __OUT64__
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "kernel/Scheduler.h"

class Out64 {
public:
    static void put(char c) { VirtualConsole::putc(Scheduler::currentConsole(), c); }
};

#endif
