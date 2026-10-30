#include "net/Net.h"
#include "kernel/KernelLog.h"
#include "fs/Err.h"
#include "net/Arp.h"
#include "net/Ethernet.h"
#include "net/Icmp.h"
#include "net/Tcp.h"
#include "net/Ipv4.h"
#include "net/LoopbackDevice.h"
#include "net/PacketQueue.h"
#include "net/NetFlag.h"
#include "net/NetInterfaceInfo.h"
#include "net/NetConfig.h"
#include "net/EtherType.h"
#include "lib/vitralKernel/String.h"
#include "net/NetDevices.h"

static PacketQueue receiveQueue(Net::RX_QUEUE_LIMIT);
static LoopbackDevice loopback;
static unsigned selfTestFrames = 0;
static uint8_t selfTestPayload[4] = {0xCA, 0xFE, 0xBA, 0xBE};

static void
selfTestHandler(NetDevice &, PacketBuffer *packet) {
    if (packet->length() >= sizeof(selfTestPayload) && Memory::compare(packet->data(), selfTestPayload, sizeof(selfTestPayload)) == 0) {
        selfTestFrames++;
    }
    PacketBuffer::release(packet);
}

bool
Net::enqueueReceived(PacketBuffer *frame) { return receiveQueue.push(frame); }

unsigned
Net::pending() { return receiveQueue.length(); }

unsigned
Net::processPending(unsigned limit) {
    unsigned handled = 0;
    while (handled < limit) {
        PacketBuffer *frame = receiveQueue.pop();
        if (!frame) {
            break;
        }
        Ethernet::input(*frame->device, frame);
        handled++;
    }
    return handled;
}

bool
Net::selfTest() {
    if (!Ethernet::registerProtocol(EtherType::Experimental, &selfTestHandler)) {
        return false;
    }
    PacketBuffer *packet = PacketBuffer::allocate();
    if (!packet) {
        return false;
    }
    Memory::copy(packet->put(sizeof(selfTestPayload)), selfTestPayload, sizeof(selfTestPayload));
    if (Ethernet::output(loopback, loopback.mac(), EtherType::Experimental, packet) != 0) {
        return false;
    }
    return processPending() == 1 && selfTestFrames == 1 && PacketBuffer::inUse() == 0;
}

int64_t
Net::describe(unsigned index, NetInterfaceInfo &out) {
    NetDevice *device = NetDevices::at(index);
    if (!device) {
        return -Err::NODEV;
    }
    String::copy(out.name, device->name(), sizeof(out.name));
    String::copy(out.driver, device->driver(), sizeof(out.driver));
    out.flags = 0;
    if (device->isUp()) {
        out.flags |= NetFlag::UP | (device->hasLink() ? NetFlag::RUNNING : 0);
    }
    if (device->isLoopback()) {
        out.flags |= NetFlag::LOOPBACK;
    } else {
        out.flags |= NetFlag::BROADCAST | NetFlag::MULTICAST;
    }
    out.mtu = device->mtu();
    Memory::copy(out.mac, device->mac().data(), MacAddress::SIZE);
    Memory::copy(out.address, device->ipv4().data(), 4);
    Memory::copy(out.netmask, device->netmask().data(), 4);
    Memory::copy(out.broadcast, device->broadcastAddress().data(), 4);
    const NetDevice::Statistics &s = device->statistics();
    out.rxPackets = s.rxPackets; out.rxBytes = s.rxBytes; out.rxErrors = s.rxErrors; out.rxDropped = s.rxDropped;
    out.txPackets = s.txPackets; out.txBytes = s.txBytes; out.txErrors = s.txErrors; out.txDropped = s.txDropped;
    return 0;
}

int64_t
Net::configure(const NetConfig &config) {
    NetDevice *device = NetDevices::at(config.index);
    if (!device) {
        return -Err::NODEV;
    }
    bool address = config.actions & NetConfig::SET_ADDRESS;
    bool netmask = config.actions & NetConfig::SET_NETMASK;
    Ipv4Address newAddress = address ? Ipv4Address(config.address[0], config.address[1], config.address[2], config.address[3])
                                     : device->ipv4();
    Ipv4Address newMask = netmask ? Ipv4Address(config.netmask[0], config.netmask[1], config.netmask[2], config.netmask[3])
                                  : device->netmask();
    if (address && (newAddress.isZero() || newAddress.isMulticast() || newAddress.isLimitedBroadcast())) {
        return -Err::INVAL;
    }
    if (netmask && !newMask.isNetmask()) {
        return -Err::INVAL;
    }
    if (address && newMask.isZero()) {
        newMask = newAddress.classfulNetmask();
    }
    Ipv4Address newBroadcast = device->broadcastAddress();
    if (address || netmask) {
        newBroadcast = device->isLoopback() ? Ipv4Address() : newAddress.broadcastFor(newMask);
    }
    if (config.actions & NetConfig::SET_BROADCAST) {
        newBroadcast = Ipv4Address(config.broadcast[0], config.broadcast[1], config.broadcast[2], config.broadcast[3]);
    }
    if (address || netmask || (config.actions & NetConfig::SET_BROADCAST)) {
        device->setIpv4(newAddress, newMask, newBroadcast);
        device->setAnnouncements(ANNOUNCEMENTS);
        Arp::flush(*device);
    }
    if (config.actions & NetConfig::BRING_DOWN) {
        device->close();
    } else if (address || (config.actions & NetConfig::BRING_UP)) {
        int status = device->open();
        if (status != 0) {
            return status;
        }
    }
    return 0;
}

void
Net::timer() {
    static unsigned ticks = 0;
    if (++ticks < 18) {
        return;
    }
    ticks = 0;
    for (unsigned i = 0; i < NetDevices::count(); i++) {
        NetDevice *device = NetDevices::at(i);
        if (device->isLoopback() || !device->isUp() || !device->hasIpv4()) {
            continue;
        }
        if (!device->hasLink()) {
            device->setAnnouncements(ANNOUNCEMENTS);
        } else if (device->announcementsLeft() > 0) {
            device->setAnnouncements(device->announcementsLeft() - 1);
            Arp::announce(*device);
        }
    }
}

void
Net::init() {
    Arp::init();
    Ipv4::init();
    Icmp::init();
    Tcp::init();
    NetDevices::add(&loopback);
    loopback.setIpv4(Ipv4Address(127, 0, 0, 1), Ipv4Address(255, 0, 0, 0), Ipv4Address());
    loopback.open();
    kprintf("Network: lo (loopback) up, %u byte packet buffers, self-test %s\n", (uint32_t)PacketBuffer::STORAGE,
             selfTest() ? "ok" : "FAILED");
}
