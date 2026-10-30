#ifndef __VITRAL_KERNEL_STRING__
#define __VITRAL_KERNEL_STRING__
#include <stddef.h>

class String {
public:
    static size_t length(const char *s);
    static bool equals(const char *a, const char *b);
    static void copy(char *dst, const char *src, size_t size);
};

#endif
