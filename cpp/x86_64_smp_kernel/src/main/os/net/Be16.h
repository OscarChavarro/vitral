#ifndef __BE16__
#define __BE16__
#include <stdint.h>
#include "net/ByteOrder.h"

class Be16 {
public:
    constexpr Be16() : raw(0) {}
    constexpr Be16(uint16_t host) : raw(ByteOrder::swap16(host)) {}
    constexpr uint16_t get() const { return ByteOrder::swap16(raw); }
    void set(uint16_t host) { raw = ByteOrder::swap16(host); }
    constexpr bool operator==(Be16 other) const { return raw == other.raw; }

private:
    uint16_t raw;
} __attribute__((packed));
static_assert(sizeof(Be16) == 2 && alignof(Be16) == 1, "Be16 is a 2-byte unaligned field");

#endif
