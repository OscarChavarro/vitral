#ifndef __STATIC_FILE__
#define __STATIC_FILE__
#include <stddef.h>

class StaticFile {
public:
    static void *open(const char *path, uint64_t &size);
    static size_t read(void *file, void *buffer, size_t size);
    static void close(void *file);
};

#endif
