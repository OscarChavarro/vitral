#ifndef __ARP_PACKET__
#define __ARP_PACKET__
#include "net/Be16.h"
#include "net/MacAddress.h"
#include "net/Ipv4Address.h"

class ArpPacket {
public:
    Be16 hardwareType;
    Be16 protocolType;
    uint8_t hardwareSize;
    uint8_t protocolSize;
    Be16 operation;
    MacAddress senderMac;
    Ipv4Address senderIp;
    MacAddress targetMac;
    Ipv4Address targetIp;
} __attribute__((packed));

#endif
