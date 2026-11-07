#ifndef __OUT64__
#define __OUT64__
#include "drivers/tty/Tty.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "kernel/Scheduler.h"

// printf64(): the calling process's terminal, or the screen during the boot, before the terminals exist.
class Out64 {
public:
    static void put(char c) {
        Tty *terminal = Tty::current();
        if (terminal) {
            terminal->write(&c, 1);
        } else {
            VirtualConsole::putc(Scheduler::currentConsole(), c);
        }
    }
};

#endif
