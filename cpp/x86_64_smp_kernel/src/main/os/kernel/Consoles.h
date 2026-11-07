#ifndef __CONSOLES__
#define __CONSOLES__
#include <stdint.h>
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "lib/vitralKernel/SpinLock.h"

class Consoles {
public:
    static constexpr unsigned COUNT = VirtualConsole::COUNT;

    static int allocate(uint32_t pid);
    static void release(unsigned console);
    static void retire(unsigned console, const char *name, int exitCode);
    static bool isEmpty(unsigned console);
    static void switchTo(unsigned console);
    // The console the screen showed before the last switch to another one.
    static unsigned previous() { return previousIndex; }

private:
    static uint32_t owner[COUNT];
    static bool retained[COUNT];
    static unsigned previousIndex;
    static SpinLock lock;
    static void showEmptyMessage(unsigned console);
};

#endif
