#include "drivers/tty/ControllingTty.h"
#include "drivers/char/CharDevices.h"
#include "drivers/tty/Tty.h"
#include "kernel/KernelLog.h"

void
ControllingTty::init() {
    if (!CharDevices::add(new ControllingTty())) {
        kprintf("tty: cannot register /dev/tty\n");
    }
}

int64_t
ControllingTty::read(void *buffer, size_t count) { return Tty::current()->read(buffer, count); }

int64_t
ControllingTty::write(const void *buffer, size_t count) { return Tty::current()->write(buffer, count); }

bool
ControllingTty::readable() { return Tty::current()->readable(); }

int64_t
ControllingTty::ioctl(uint64_t request, void *argument) { return Tty::current()->ioctl(request, argument); }

Tty *
ControllingTty::asTty() { return Tty::current(); }
