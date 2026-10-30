#ifndef __INIT__
#define __INIT__
#include "kernel/ArgList.h"

class Init {
public:
    static int main(const ArgList &args);

private:
    static constexpr uint32_t FAILURE_PAUSE_MS = 8000;
    static constexpr uint32_t RESTART_PAUSE_MS = 1000;
};

#endif
