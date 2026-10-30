#ifndef __MAC_ADDRESS__
#define __MAC_ADDRESS__
#include <stdint.h>
#include "lib/vitralKernel/Memory.h"

class MacAddress {
public:
    static constexpr unsigned SIZE = 6;
    static constexpr unsigned TEXT_SIZE = 18;

    constexpr MacAddress() : bytes{0, 0, 0, 0, 0, 0} {}
    constexpr MacAddress(uint8_t a, uint8_t b, uint8_t c, uint8_t d, uint8_t e, uint8_t f) : bytes{a, b, c, d, e, f} {}

    static constexpr MacAddress broadcast() { return MacAddress(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF); }
    static MacAddress from(const void *raw) {
        MacAddress mac;
        Memory::copy(mac.bytes, raw, SIZE);
        return mac;
    }

    bool operator==(const MacAddress &other) const { return Memory::compare(bytes, other.bytes, SIZE) == 0; }
    bool operator!=(const MacAddress &other) const { return !(*this == other); }

    bool isBroadcast() const { return *this == broadcast(); }
    bool isMulticast() const { return bytes[0] & 1; }
    bool isZero() const { return *this == MacAddress(); }
    bool isLocal() const { return bytes[0] & 2; }

    uint8_t byte(unsigned i) const { return bytes[i]; }
    const uint8_t *data() const { return bytes; }

    void format(char *text) const {
        static constexpr char HEX[] = "0123456789abcdef";
        for (unsigned i = 0; i < SIZE; i++) {
            text[i * 3] = HEX[bytes[i] >> 4];
            text[i * 3 + 1] = HEX[bytes[i] & 15];
            text[i * 3 + 2] = i + 1 < SIZE ? ':' : '\0';
        }
    }

private:
    uint8_t bytes[SIZE];
};

static_assert(sizeof(MacAddress) == 6 && alignof(MacAddress) == 1, "MacAddress goes straight into frames");

#endif
