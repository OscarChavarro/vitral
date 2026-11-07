#ifndef __REBOOT_COMMAND__
#define __REBOOT_COMMAND__
#include "kernel/ArgList.h"

class RebootCommand {
public:
    static int main(const ArgList &args);

private:
    // Lets the message reach the screen and COM1 before the machine resets.
    static constexpr uint32_t FLUSH_MS = 200;
};

#endif
