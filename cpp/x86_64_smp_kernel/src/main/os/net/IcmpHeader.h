#ifndef __ICMP_HEADER__
#define __ICMP_HEADER__
#include <stdint.h>
#include "net/Be16.h"

class IcmpHeader {
public:
    uint8_t type;
    uint8_t code;
    Be16 checksum;
    Be16 identifier;
    Be16 sequence;
} __attribute__((packed));

#endif
