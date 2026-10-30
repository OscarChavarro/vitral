#ifndef __BLOCK_IOCTL__
#define __BLOCK_IOCTL__
#include <stdint.h>

class BlockIoctl {
public:
    static constexpr uint64_t SET_READ_ONLY = 0x125D;
    static constexpr uint64_t GET_READ_ONLY = 0x125E;
    static constexpr uint64_t GET_SIZE = 0x1260;
    static constexpr uint64_t FLUSH = 0x1261;
    static constexpr uint64_t GET_SECTOR_SIZE = 0x1268;
    static constexpr uint64_t GET_SIZE_BYTES = 0x80081272;
};

#endif
