#ifndef __NET_FLAG__
#define __NET_FLAG__
#include <stdint.h>

class NetFlag {
public:
    static constexpr uint32_t UP = 0x1;
    static constexpr uint32_t BROADCAST = 0x2;
    static constexpr uint32_t LOOPBACK = 0x8;
    static constexpr uint32_t RUNNING = 0x40;
    static constexpr uint32_t MULTICAST = 0x1000;
};

#endif
