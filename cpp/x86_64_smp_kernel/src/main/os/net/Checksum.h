#ifndef __CHECKSUM__
#define __CHECKSUM__
#include <stdint.h>
#include <stddef.h>

class Checksum {
public:
    static uint32_t add(uint32_t sum, const void *data, size_t length);
    static uint16_t finish(uint32_t sum);
    static uint16_t compute(const void *data, size_t length) { return finish(add(0, data, length)); }
};

#endif
