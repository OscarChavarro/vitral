#include "net/Arp.h"
#include "kernel/Clock.h"
#include "net/ArpPacket.h"
#include "lib/vitralKernel/LockGuard.h"
#include "net/EtherType.h"

Arp::Entry Arp::cache[Arp::CACHE_SIZE];
unsigned Arp::victim = 0;
SpinLock Arp::lock;

void
Arp::init() { Ethernet::registerProtocol(EtherType::Arp, &Arp::input); }

bool
Arp::lookup(const NetDevice &device, const Ipv4Address &ip, MacAddress &mac) {
    LockGuard guard(lock);
    for (const Entry &e : cache) {
        if (e.used && e.device == &device && e.ip == ip && Clock::ticks() - e.learnedAt < Clock::msToTicks(EXPIRY_MS)) {
            mac = e.mac;
            return true;
        }
    }
    return false;
}

void
Arp::learn(const NetDevice &device, const Ipv4Address &ip, const MacAddress &mac, bool mayCreate) {
    LockGuard guard(lock);
    for (Entry &e : cache) {
        if (e.used && e.device == &device && e.ip == ip) {
            e.mac = mac;
            e.learnedAt = Clock::ticks();
            return;
        }
    }
    if (!mayCreate) {
        return;
    }
    for (Entry &e : cache) {
        if (!e.used) {
            e = {true, &device, ip, mac, Clock::ticks()};
            return;
        }
    }
    cache[victim] = {true, &device, ip, mac, Clock::ticks()};
    victim = (victim + 1) % CACHE_SIZE;
}

void
Arp::flush(const NetDevice &device) {
    LockGuard guard(lock);
    for (Entry &e : cache) {
        if (e.used && e.device == &device) {
            e.used = false;
        }
    }
}

void
Arp::announce(NetDevice &device) { send(device, REQUEST, MacAddress(), MacAddress::broadcast(), device.ipv4()); }

bool
Arp::resolve(NetDevice &device, const Ipv4Address &ip, MacAddress &mac) {
    if (lookup(device, ip, mac)) {
        return true;
    }
    send(device, REQUEST, MacAddress(), MacAddress::broadcast(), ip);
    return false;
}

void
Arp::send(NetDevice &device, uint16_t operation, const MacAddress &targetMac, const MacAddress &ethernetDestination,
          const Ipv4Address &targetIp) {
    PacketBuffer *packet = PacketBuffer::allocate();
    if (!packet) {
        return;
    }
    ArpPacket *arp = (ArpPacket *)packet->put(sizeof(ArpPacket));
    arp->hardwareType.set(1);
    arp->protocolType.set((uint16_t)EtherType::Ipv4);
    arp->hardwareSize = MacAddress::SIZE;
    arp->protocolSize = 4;
    arp->operation.set(operation);
    arp->senderMac = device.mac();
    arp->senderIp = device.ipv4();
    arp->targetMac = targetMac;
    arp->targetIp = targetIp;
    Ethernet::output(device, ethernetDestination, EtherType::Arp, packet);
}

void
Arp::input(NetDevice &device, PacketBuffer *packet) {
    if (packet->length() >= sizeof(ArpPacket) && device.hasIpv4()) {
        ArpPacket arp = *packet->as<ArpPacket>();
        bool valid = arp.hardwareType.get() == 1 && arp.protocolType.get() == (uint16_t)EtherType::Ipv4 &&
                     arp.hardwareSize == MacAddress::SIZE && arp.protocolSize == 4 && !arp.senderMac.isMulticast() &&
                     !arp.senderIp.isZero();
        if (valid) {
            bool forUs = arp.targetIp == device.ipv4();
            learn(device, arp.senderIp, arp.senderMac, forUs);
            if (forUs && arp.operation.get() == REQUEST) {
                send(device, REPLY, arp.senderMac, arp.senderMac, arp.senderIp);
            }
        }
    }
    PacketBuffer::release(packet);
}
