#ifndef __INODE__
#define __INODE__
#include <stdint.h>

class BlockDevice;
class CharDevice;
class Filesystem;

class Inode {
public:
    Filesystem *fs = nullptr;
    uint32_t mode = 0;
    uint64_t size = 0;
    uint64_t ino = 0;
    uint64_t aux0 = 0;
    uint64_t aux1 = 0;
    BlockDevice *device = nullptr;
    CharDevice *charDevice = nullptr;
};

#endif
