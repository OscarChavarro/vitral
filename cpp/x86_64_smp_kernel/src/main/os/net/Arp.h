#ifndef __ARP__
#define __ARP__
#include "lib/vitralKernel/SpinLock.h"
#include "net/ByteOrder.h"
#include "net/Ethernet.h"
#include "net/Ipv4Address.h"
#include "net/NetDevice.h"
#include "net/Be16.h"

class Arp {
public:
    static constexpr unsigned CACHE_SIZE = 16;
    static constexpr uint64_t EXPIRY_MS = 5 * 60 * 1000;
    static constexpr uint16_t REQUEST = 1;
    static constexpr uint16_t REPLY = 2;

    static void init();

    static void learn(const NetDevice &device, const Ipv4Address &ip, const MacAddress &mac, bool mayCreate);

    static void announce(NetDevice &device);

    static bool resolve(NetDevice &device, const Ipv4Address &ip, MacAddress &mac);
    static bool lookup(const NetDevice &device, const Ipv4Address &ip, MacAddress &mac);
    static void flush(const NetDevice &device);

private:
    class Entry {
    public:
        bool used;
        const NetDevice *device;
        Ipv4Address ip;
        MacAddress mac;
        uint64_t learnedAt;
    };
    static Entry cache[CACHE_SIZE];
    static unsigned victim;
    static SpinLock lock;

    static void input(NetDevice &device, PacketBuffer *packet);
    static void send(NetDevice &device, uint16_t operation, const MacAddress &targetMac, const MacAddress &ethernetDestination,
                     const Ipv4Address &targetIp);
};

#endif
