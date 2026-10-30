#ifndef __FORMAT__
#define __FORMAT__
#include <stddef.h>

class Format {
public:
    static int print(char *buffer, size_t capacity, const char *fmt, ...);
};

#endif
