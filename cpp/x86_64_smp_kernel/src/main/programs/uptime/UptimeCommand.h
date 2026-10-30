#ifndef __UPTIME_COMMAND__
#define __UPTIME_COMMAND__
#include "kernel/ArgList.h"

class UptimeCommand {
public:
    static int main(const ArgList &args);
};

#endif
