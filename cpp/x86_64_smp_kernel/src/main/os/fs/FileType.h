#ifndef __FILE_TYPE__
#define __FILE_TYPE__
#include <stdint.h>

class FileType {
public:
    static constexpr uint32_t MASK = 0170000;
    static constexpr uint32_t DIRECTORY = 0040000;
    static constexpr uint32_t CHAR_DEVICE = 0020000;
    static constexpr uint32_t BLOCK_DEVICE = 0060000;
    static constexpr uint32_t REGULAR = 0100000;
};

#endif
