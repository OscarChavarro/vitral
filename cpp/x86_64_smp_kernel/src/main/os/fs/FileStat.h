#ifndef __FILE_STAT__
#define __FILE_STAT__
#include <stdint.h>

class FileStat {
public:
    uint64_t device;
    uint64_t inode;
    uint32_t mode;
    uint32_t blockSize;
    uint64_t size;
    uint64_t blocks;
    uint64_t rdev;
};

#endif
