#ifndef __MEMORY__
#define __MEMORY__
#include <stddef.h>

class Memory {
public:
    static void *copy(void *dst, const void *src, size_t n);
    static void *move(void *dst, const void *src, size_t n);
    static void *set(void *dst, int byte, size_t n);
    static int compare(const void *a, const void *b, size_t n);
};

#endif
