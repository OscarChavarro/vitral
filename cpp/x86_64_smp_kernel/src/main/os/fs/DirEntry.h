#ifndef __DIR_ENTRY__
#define __DIR_ENTRY__
#include <stdint.h>

class DirEntry {
public:
    static constexpr unsigned NAME_MAX = 255;
    uint64_t ino;
    uint32_t mode;
    char name[NAME_MAX + 1];
};

#endif
