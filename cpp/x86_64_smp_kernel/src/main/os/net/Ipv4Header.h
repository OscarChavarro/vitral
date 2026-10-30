#ifndef __IPV4_HEADER__
#define __IPV4_HEADER__
#include <stdint.h>
#include "net/Be16.h"
#include "net/Ipv4Address.h"

class Ipv4Header {
public:
    uint8_t versionAndLength;
    uint8_t typeOfService;
    Be16 totalLength;
    Be16 identification;
    Be16 flagsAndFragment;
    uint8_t timeToLive;
    uint8_t protocol;
    Be16 checksum;
    Ipv4Address source;
    Ipv4Address destination;

    unsigned headerLength() const { return (versionAndLength & 15) * 4u; }
} __attribute__((packed));

#endif
