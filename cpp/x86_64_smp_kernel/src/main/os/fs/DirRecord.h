#ifndef __DIR_RECORD__
#define __DIR_RECORD__
#include <stdint.h>

class DirRecord {
public:
    static constexpr uint8_t TYPE_UNKNOWN = 0;
    static constexpr uint8_t TYPE_DIRECTORY = 4;
    static constexpr uint8_t TYPE_BLOCK = 6;
    static constexpr uint8_t TYPE_CHAR = 2;
    static constexpr uint8_t TYPE_REGULAR = 8;
    uint64_t inode;
    int64_t offset;
    uint16_t length;
    uint8_t type;
    char name[1];
    static constexpr unsigned HEADER = 19;
};

#endif
