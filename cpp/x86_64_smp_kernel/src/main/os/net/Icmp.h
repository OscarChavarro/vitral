#ifndef __ICMP__
#define __ICMP__
#include "net/Ipv4.h"
#include "net/Be16.h"
#include "net/Ipv4Header.h"

class Icmp {
public:
    static constexpr uint8_t ECHO_REPLY = 0;
    static constexpr uint8_t ECHO_REQUEST = 8;

    static void init();

    static unsigned echoRepliesSent() { return repliesSent; }

private:
    static unsigned repliesSent;
    static void input(NetDevice &device, PacketBuffer *packet, const Ipv4Header &ip);
};

#endif
