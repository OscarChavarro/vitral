#ifndef __NET_CONFIG__
#define __NET_CONFIG__
#include <stdint.h>

class NetConfig {
public:
    static constexpr uint32_t SET_ADDRESS = 1;
    static constexpr uint32_t SET_NETMASK = 2;
    static constexpr uint32_t SET_BROADCAST = 4;
    static constexpr uint32_t BRING_UP = 8;
    static constexpr uint32_t BRING_DOWN = 16;
    uint32_t index;
    uint32_t actions;
    uint8_t address[4];
    uint8_t netmask[4];
    uint8_t broadcast[4];
};

#endif
