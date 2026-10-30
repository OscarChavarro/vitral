#include "net/Ipv4.h"
#include "fs/Err.h"
#include "net/Arp.h"
#include "net/Checksum.h"
#include "net/IpProtocol.h"
#include "net/Ipv4Header.h"
#include "net/EtherType.h"

Ipv4::Entry Ipv4::protocols[Ipv4::MAX_PROTOCOLS];
unsigned Ipv4::protocolCount = 0;
uint16_t Ipv4::nextIdentification = 1;

void
Ipv4::init() { Ethernet::registerProtocol(EtherType::Ipv4, &Ipv4::input); }

bool
Ipv4::registerProtocol(IpProtocol protocol, Handler handler) {
    for (unsigned i = 0; i < protocolCount; i++) {
        if (protocols[i].protocol == protocol) {
            return false;
        }
    }
    if (protocolCount >= MAX_PROTOCOLS) {
        return false;
    }
    protocols[protocolCount++] = {protocol, handler};
    return true;
}

void
Ipv4::input(NetDevice &device, PacketBuffer *packet) {
    if (!device.hasIpv4() || packet->length() < HEADER_SIZE) {
        PacketBuffer::release(packet);
        return;
    }
    Ipv4Header header = *packet->as<Ipv4Header>();
    unsigned headerLength = header.headerLength();
    unsigned total = header.totalLength.get();
    bool valid = (header.versionAndLength >> 4) == 4 && headerLength >= HEADER_SIZE && headerLength <= packet->length() &&
                 total >= headerLength && total <= packet->length() &&
                 Checksum::compute(packet->data(), headerLength) == 0 &&
                 (header.flagsAndFragment.get() & 0x3FFF) == 0;
    bool forUs = header.destination == device.ipv4() || header.destination.isLimitedBroadcast() ||
                 (device.hasBroadcast() && header.destination == device.broadcastAddress());
    if (!valid || !forUs) {
        PacketBuffer::release(packet);
        return;
    }
    if (device.needsArp() && !header.source.isZero() && !header.source.isLimitedBroadcast() && !header.source.isMulticast() &&
        header.source.sameNetwork(device.ipv4(), device.netmask()) && !(device.hasBroadcast() && header.source == device.broadcastAddress())) {
        Arp::learn(device, header.source, MacAddress::from(packet->senderMac), true);
    }
    packet->trim(total);
    packet->pull(headerLength);
    for (unsigned i = 0; i < protocolCount; i++) {
        if ((uint8_t)protocols[i].protocol == header.protocol) {
            protocols[i].handler(device, packet, header);
            return;
        }
    }
    PacketBuffer::release(packet);
}

int
Ipv4::output(NetDevice &device, const Ipv4Address &destination, IpProtocol protocol, PacketBuffer *packet) {
    size_t total = packet->length() + HEADER_SIZE;
    if (!device.hasIpv4() || total > device.mtu()) {
        PacketBuffer::release(packet);
        return -Err::INVAL;
    }
    MacAddress mac;
    if (!device.needsArp()) {
        mac = device.mac();
    } else if (destination.isLimitedBroadcast() || (device.hasBroadcast() && destination == device.broadcastAddress())) {
        mac = MacAddress::broadcast();
    } else if (!destination.sameNetwork(device.ipv4(), device.netmask())) {
        PacketBuffer::release(packet);
        return -Err::NETUNREACH;
    } else if (!Arp::resolve(device, destination, mac)) {
        PacketBuffer::release(packet);
        return -Err::HOSTUNREACH;
    }
    Ipv4Header *header = (Ipv4Header *)packet->push(HEADER_SIZE);
    if (!header) {
        PacketBuffer::release(packet);
        return -Err::INVAL;
    }
    header->versionAndLength = 0x45;
    header->typeOfService = 0;
    header->totalLength.set((uint16_t)total);
    header->identification.set(__atomic_fetch_add(&nextIdentification, 1, __ATOMIC_RELAXED));
    header->flagsAndFragment.set(0);
    header->timeToLive = DEFAULT_TTL;
    header->protocol = (uint8_t)protocol;
    header->checksum.set(0);
    header->source = device.ipv4();
    header->destination = destination;
    header->checksum.set(Checksum::compute(header, HEADER_SIZE));
    return Ethernet::output(device, mac, EtherType::Ipv4, packet);
}
