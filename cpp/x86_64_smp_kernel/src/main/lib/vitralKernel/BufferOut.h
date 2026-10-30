#ifndef __BUFFER_OUT__
#define __BUFFER_OUT__
#include <stddef.h>

class BufferOut {
public:
    static inline char *dst = nullptr;
    static inline size_t room = 0;
    static void put(char c) {
        if (room > 1) { *dst++ = c; room--; }
    }
};

#endif
