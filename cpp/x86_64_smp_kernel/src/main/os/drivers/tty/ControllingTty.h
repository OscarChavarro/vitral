#ifndef __CONTROLLING_TTY__
#define __CONTROLLING_TTY__
#include <stddef.h>
#include <stdint.h>
#include "drivers/char/CharDevice.h"

// /dev/tty: whichever terminal the calling process has, as on Unix.
class ControllingTty : public CharDevice {
public:
    static void init();

    int64_t read(void *buffer, size_t count) override;
    int64_t write(const void *buffer, size_t count) override;
    bool readable() override;
    int64_t ioctl(uint64_t request, void *argument) override;
    Tty *asTty() override;

private:
    ControllingTty() : CharDevice("", "tty") {}
};

#endif
