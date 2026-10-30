#include "net/Ethernet.h"
#include "fs/Err.h"
#include "net/NetDevice.h"
#include "net/EtherType.h"
#include "net/EthernetHeader.h"

Ethernet::Entry Ethernet::protocols[Ethernet::MAX_PROTOCOLS];
unsigned Ethernet::protocolCount = 0;
unsigned Ethernet::unknownProtocol = 0;

bool
Ethernet::registerProtocol(EtherType type, Handler handler) {
    for (unsigned i = 0; i < protocolCount; i++) {
        if (protocols[i].type == type) {
            return false;
        }
    }
    if (protocolCount >= MAX_PROTOCOLS) {
        return false;
    }
    protocols[protocolCount++] = {type, handler};
    return true;
}

void
Ethernet::input(NetDevice &device, PacketBuffer *frame) {
    if (frame->length() < HEADER_SIZE) {
        device.countRxError();
        PacketBuffer::release(frame);
        return;
    }
    EthernetHeader header = *frame->as<EthernetHeader>();
    bool forUs = header.destination == device.mac() || header.destination.isMulticast();
    if (!forUs) {
        PacketBuffer::release(frame);
        return;
    }
    Memory::copy(frame->senderMac, header.source.data(), MacAddress::SIZE);
    frame->pull(HEADER_SIZE);
    EtherType type = (EtherType)header.type.get();
    for (unsigned i = 0; i < protocolCount; i++) {
        if (protocols[i].type == type) {
            protocols[i].handler(device, frame);
            return;
        }
    }
    __atomic_fetch_add(&unknownProtocol, 1, __ATOMIC_RELAXED);
    PacketBuffer::release(frame);
}

int
Ethernet::output(NetDevice &device, const MacAddress &destination, EtherType type, PacketBuffer *packet) {
    EthernetHeader *header = (EthernetHeader *)packet->push(HEADER_SIZE);
    if (!header) {
        PacketBuffer::release(packet);
        return -Err::INVAL;
    }
    header->destination = destination;
    header->source = device.mac();
    header->type.set((uint16_t)type);
    if (packet->length() < MIN_FRAME) {
        size_t missing = MIN_FRAME - packet->length();
        uint8_t *padding = packet->put(missing);
        if (!padding) {
            PacketBuffer::release(packet);
            return -Err::INVAL;
        }
        Memory::set(padding, 0, missing);
    }
    return device.transmit(packet);
}
