#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "lib/svgalib/SvgalibMouse.h"
#include "syscalls/Syscalls.h"

SvgalibMouse::State &
SvgalibMouse::contextState() {
    static State states[VirtualConsole::COUNT];
    return states[Syscalls::console() % VirtualConsole::COUNT];
}
