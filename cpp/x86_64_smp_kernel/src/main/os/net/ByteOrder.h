#ifndef __NET_BYTE_ORDER__
#define __NET_BYTE_ORDER__
#include <stdint.h>

class ByteOrder {
public:
    static constexpr uint16_t swap16(uint16_t v) { return (uint16_t)(v << 8 | v >> 8); }
    static constexpr uint32_t swap32(uint32_t v) {
        return v << 24 | (v & 0xFF00u) << 8 | (v >> 8 & 0xFF00u) | v >> 24;
    }
    static constexpr uint16_t hostToNet16(uint16_t v) { return swap16(v); }
    static constexpr uint32_t hostToNet32(uint32_t v) { return swap32(v); }
    static constexpr uint16_t netToHost16(uint16_t v) { return swap16(v); }
    static constexpr uint32_t netToHost32(uint32_t v) { return swap32(v); }
};

#endif
