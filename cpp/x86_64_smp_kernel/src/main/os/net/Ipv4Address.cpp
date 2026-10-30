#include "net/Ipv4Address.h"

bool
Ipv4Address::parse(const char *text, Ipv4Address &out) {
    uint8_t parts[4];
    for (unsigned i = 0; i < 4; i++) {
        if (*text < '0' || *text > '9') {
            return false;
        }
        unsigned value = 0;
        unsigned digits = 0;
        for (; *text >= '0' && *text <= '9'; text++, digits++) {
            value = value * 10 + (unsigned)(*text - '0');
            if (value > 255 || digits >= 3) {
                return false;
            }
        }
        parts[i] = (uint8_t)value;
        if (i < 3) {
            if (*text != '.') {
                return false;
            }
            text++;
        }
    }
    if (*text != '\0') {
        return false;
    }
    out = Ipv4Address(parts[0], parts[1], parts[2], parts[3]);
    return true;
}

void
Ipv4Address::format(char *text) const {
    for (unsigned i = 0; i < 4; i++) {
        unsigned v = bytes[i];
        if (v >= 100) {
            *text++ = (char)('0' + v / 100);
        }
        if (v >= 10) {
            *text++ = (char)('0' + v / 10 % 10);
        }
        *text++ = (char)('0' + v % 10);
        *text++ = i < 3 ? '.' : '\0';
    }
}
