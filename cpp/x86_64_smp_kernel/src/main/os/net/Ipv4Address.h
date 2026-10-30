#ifndef __IPV4_ADDRESS__
#define __IPV4_ADDRESS__
#include <stdint.h>

class Ipv4Address {
public:
    static constexpr unsigned TEXT_SIZE = 16;

    constexpr Ipv4Address() : bytes{0, 0, 0, 0} {}
    constexpr Ipv4Address(uint8_t a, uint8_t b, uint8_t c, uint8_t d) : bytes{a, b, c, d} {}
    static constexpr Ipv4Address fromHost(uint32_t v) {
        return Ipv4Address((uint8_t)(v >> 24), (uint8_t)(v >> 16), (uint8_t)(v >> 8), (uint8_t)v);
    }
    static constexpr Ipv4Address broadcast() { return Ipv4Address(255, 255, 255, 255); }

    constexpr uint32_t host() const {
        return (uint32_t)bytes[0] << 24 | (uint32_t)bytes[1] << 16 | (uint32_t)bytes[2] << 8 | bytes[3];
    }
    constexpr bool operator==(const Ipv4Address &o) const { return host() == o.host(); }
    constexpr bool operator!=(const Ipv4Address &o) const { return host() != o.host(); }

    constexpr bool isZero() const { return host() == 0; }
    constexpr bool isLimitedBroadcast() const { return host() == 0xFFFFFFFFu; }
    constexpr bool isMulticast() const { return (bytes[0] & 0xF0) == 0xE0; }
    constexpr bool isLoopback() const { return bytes[0] == 127; }

    constexpr bool isNetmask() const {
        uint32_t inverted = ~host();
        return (inverted & (inverted + 1)) == 0;
    }
    constexpr Ipv4Address classfulNetmask() const {
        return bytes[0] < 128 ? Ipv4Address(255, 0, 0, 0) : bytes[0] < 192 ? Ipv4Address(255, 255, 0, 0)
                                                                          : Ipv4Address(255, 255, 255, 0);
    }
    constexpr Ipv4Address broadcastFor(const Ipv4Address &netmask) const { return fromHost(host() | ~netmask.host()); }
    constexpr bool sameNetwork(const Ipv4Address &other, const Ipv4Address &netmask) const {
        return ((host() ^ other.host()) & netmask.host()) == 0;
    }

    uint8_t byte(unsigned i) const { return bytes[i]; }
    const uint8_t *data() const { return bytes; }

    static bool parse(const char *text, Ipv4Address &out);
    void format(char *text) const;

private:
    uint8_t bytes[4];
};

static_assert(sizeof(Ipv4Address) == 4 && alignof(Ipv4Address) == 1, "Ipv4Address goes straight into headers");

#endif
