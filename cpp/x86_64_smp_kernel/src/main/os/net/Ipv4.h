#ifndef __IPV4__
#define __IPV4__
#include "net/ByteOrder.h"
#include "net/Ethernet.h"
#include "net/Ipv4Address.h"
#include "net/NetDevice.h"
#include "net/Be16.h"
#include "net/IpProtocol.h"
#include "net/Ipv4Header.h"

class Ipv4 {
public:
    static constexpr size_t HEADER_SIZE = sizeof(Ipv4Header);
    static constexpr uint8_t DEFAULT_TTL = 64;
    static constexpr unsigned MAX_PROTOCOLS = 8;

    using Handler = void (*)(NetDevice &device, PacketBuffer *packet, const Ipv4Header &header);

    static void init();
    static bool registerProtocol(IpProtocol protocol, Handler handler);

    static int output(NetDevice &device, const Ipv4Address &destination, IpProtocol protocol, PacketBuffer *packet);

private:
    class Entry {
    public:
        IpProtocol protocol;
        Handler handler;
    };
    static Entry protocols[MAX_PROTOCOLS];
    static unsigned protocolCount;
    static uint16_t nextIdentification;

    static void input(NetDevice &device, PacketBuffer *packet);
};

#endif
