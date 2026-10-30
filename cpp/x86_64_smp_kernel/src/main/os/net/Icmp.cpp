#include "net/Icmp.h"
#include "net/Checksum.h"
#include "net/IcmpHeader.h"
#include "net/IpProtocol.h"
#include "net/Ipv4Header.h"

unsigned Icmp::repliesSent = 0;

void
Icmp::init() { Ipv4::registerProtocol(IpProtocol::Icmp, &Icmp::input); }

void
Icmp::input(NetDevice &device, PacketBuffer *packet, const Ipv4Header &ip) {
    bool echo = packet->length() >= sizeof(IcmpHeader) && Checksum::compute(packet->data(), packet->length()) == 0 &&
                packet->as<IcmpHeader>()->type == ECHO_REQUEST && packet->as<IcmpHeader>()->code == 0;
    bool toUs = ip.destination == device.ipv4();
    if (!echo || !toUs) {
        PacketBuffer::release(packet);
        return;
    }
    IcmpHeader *icmp = packet->as<IcmpHeader>();
    icmp->type = ECHO_REPLY;
    icmp->checksum.set(0);
    icmp->checksum.set(Checksum::compute(packet->data(), packet->length()));
    if (Ipv4::output(device, ip.source, IpProtocol::Icmp, packet) == 0) {
        __atomic_fetch_add(&repliesSent, 1, __ATOMIC_RELAXED);
    }
}
