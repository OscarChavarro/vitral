#ifndef __BE32__
#define __BE32__
#include <stdint.h>
#include "net/ByteOrder.h"

class Be32 {
public:
    constexpr Be32() : raw(0) {}
    constexpr Be32(uint32_t host) : raw(ByteOrder::swap32(host)) {}
    constexpr uint32_t get() const { return ByteOrder::swap32(raw); }
    void set(uint32_t host) { raw = ByteOrder::swap32(host); }
    constexpr bool operator==(Be32 other) const { return raw == other.raw; }

private:
    uint32_t raw;
} __attribute__((packed));
static_assert(sizeof(Be32) == 4 && alignof(Be32) == 1, "Be32 is a 4-byte unaligned field");

#endif
