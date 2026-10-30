#ifndef __OPEN_FILE__
#define __OPEN_FILE__
#include <stdint.h>
#include "fs/Inode.h"

class OpenFile {
public:
    Inode inode;
    uint64_t position = 0;
    int flags = 0;
    char path[256];
    unsigned phase = 0;
    uint64_t cursor = 0;
    unsigned mountCursor = 0;
};

#endif
