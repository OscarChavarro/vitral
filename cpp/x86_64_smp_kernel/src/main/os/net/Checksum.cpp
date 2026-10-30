#include "net/Checksum.h"

uint32_t
Checksum::add(uint32_t sum, const void *data, size_t length) {
    const uint8_t *p = (const uint8_t *)data;
    uint64_t wide = sum;
    for (; length >= 2; length -= 2, p += 2) {
        wide += (uint32_t)p[0] << 8 | p[1];
    }
    if (length) {
        wide += (uint32_t)p[0] << 8;
    }
    while (wide >> 32) {
        wide = (wide & 0xFFFFFFFFu) + (wide >> 32);
    }
    return (uint32_t)wide;
}

uint16_t
Checksum::finish(uint32_t sum) {
    while (sum >> 16) {
        sum = (sum & 0xFFFF) + (sum >> 16);
    }
    return (uint16_t)~sum;
}
