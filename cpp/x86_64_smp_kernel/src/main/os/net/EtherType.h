#ifndef __ETHER_TYPE__
#define __ETHER_TYPE__
#include <stdint.h>

enum class EtherType : uint16_t {
    Ipv4 = 0x0800,
    Arp = 0x0806,
    Ipv6 = 0x86DD,
    Experimental = 0x88B5,
};

#endif
