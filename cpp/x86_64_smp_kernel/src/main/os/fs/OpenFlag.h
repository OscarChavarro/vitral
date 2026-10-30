#ifndef __OPEN_FLAG__
#define __OPEN_FLAG__
#include <stdint.h>

class OpenFlag {
public:
    static constexpr int RDONLY = 0;
    static constexpr int WRONLY = 1;
    static constexpr int RDWR = 2;
    static constexpr int ACCESS_MASK = 3;
    static constexpr int CREAT = 0100;
    static constexpr int EXCL = 0200;
    static constexpr int TRUNC = 01000;
    static constexpr int APPEND = 02000;
    static constexpr int DIRECTORY = 0200000;
};

#endif
