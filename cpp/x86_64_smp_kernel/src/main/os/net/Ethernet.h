#ifndef __ETHERNET__
#define __ETHERNET__
#include <stdint.h>
#include "net/ByteOrder.h"
#include "net/MacAddress.h"
#include "net/PacketBuffer.h"
#include "net/Be16.h"
#include "net/EtherType.h"
#include "net/EthernetHeader.h"

class NetDevice;

class Ethernet {
public:
    static constexpr size_t HEADER_SIZE = sizeof(EthernetHeader);
    static constexpr size_t MIN_FRAME = 60;
    static constexpr unsigned MAX_PROTOCOLS = 8;

    using Handler = void (*)(NetDevice &device, PacketBuffer *packet);

    static bool registerProtocol(EtherType type, Handler handler);

    static void input(NetDevice &device, PacketBuffer *frame);

    static int output(NetDevice &device, const MacAddress &destination, EtherType type, PacketBuffer *packet);

    static unsigned unknownProtocolFrames() { return unknownProtocol; }

private:
    class Entry {
    public:
        EtherType type;
        Handler handler;
    };
    static Entry protocols[MAX_PROTOCOLS];
    static unsigned protocolCount;
    static unsigned unknownProtocol;
};

#endif
