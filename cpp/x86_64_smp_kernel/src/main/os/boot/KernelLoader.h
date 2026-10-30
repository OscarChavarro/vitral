#ifndef __KERNEL_LOADER__
#define __KERNEL_LOADER__
#include "boot/BootInfo.h"

void
copyHigh(uint32_t dest, uint32_t bytes);

class KernelLoader {
public:
    static bool load(BootInfo &info);
};

#endif
