#ifndef __BLOCK_DEVICES__
#define __BLOCK_DEVICES__
#include "drivers/block/BlockDevice.h"

class BlockDevices {
public:
    static constexpr unsigned MAX = 8;

    static void init();
    static bool add(BlockDevice *device);
    static unsigned count();
    static BlockDevice *at(unsigned index);
    static BlockDevice *find(const char *name);

private:
    static BlockDevice *table[MAX];
    static unsigned total;
};

#endif
