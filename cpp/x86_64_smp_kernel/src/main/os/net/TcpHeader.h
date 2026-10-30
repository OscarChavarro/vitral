#ifndef __TCP_HEADER__
#define __TCP_HEADER__
#include <stdint.h>
#include "net/Be16.h"
#include "net/Be32.h"

class TcpHeader {
public:
    Be16 sourcePort;
    Be16 destinationPort;
    Be32 sequence;
    Be32 acknowledgment;
    uint8_t dataOffset;
    uint8_t flags;
    Be16 window;
    Be16 checksum;
    Be16 urgent;

    unsigned headerLength() const { return (dataOffset >> 4) * 4u; }
} __attribute__((packed));
static_assert(sizeof(TcpHeader) == 20, "TCP header is 20 bytes");

#endif
