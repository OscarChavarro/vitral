#ifndef __KERNEL_HEADER__
#define __KERNEL_HEADER__
#include <stdint.h>

class KernelHeader {
public:
    uint32_t magic;
    uint32_t imageBytes;
    uint32_t entry;
    uint32_t bssEnd;
    static constexpr uint32_t MAGIC = 0x4E524B56;
};

#endif
