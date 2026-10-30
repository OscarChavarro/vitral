#ifndef __IP_PROTOCOL__
#define __IP_PROTOCOL__
#include <stdint.h>

enum class IpProtocol : uint8_t {
    Icmp = 1,
    Tcp = 6,
    Udp = 17,
};

#endif
