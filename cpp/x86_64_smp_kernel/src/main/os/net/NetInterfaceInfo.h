#ifndef __NET_INTERFACE_INFO__
#define __NET_INTERFACE_INFO__
#include <stdint.h>

class NetInterfaceInfo {
public:
    char name[16];
    char driver[16];
    uint32_t flags;
    uint32_t mtu;
    uint8_t mac[6];
    uint8_t address[4];
    uint8_t netmask[4];
    uint8_t broadcast[4];
    uint64_t rxPackets;
    uint64_t rxBytes;
    uint64_t rxErrors;
    uint64_t rxDropped;
    uint64_t txPackets;
    uint64_t txBytes;
    uint64_t txErrors;
    uint64_t txDropped;
};

#endif
