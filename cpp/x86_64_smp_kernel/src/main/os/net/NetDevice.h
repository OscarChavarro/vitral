#ifndef __NET_DEVICE__
#define __NET_DEVICE__
#include <stdint.h>
#include "net/Ipv4Address.h"
#include "net/MacAddress.h"
#include "net/PacketBuffer.h"

class NetDevice {
public:
    static constexpr unsigned NAME_LENGTH = 16;
    static constexpr uint16_t ETHERNET_MTU = 1500;

    // Updated with 64-bit atomics: 8-byte alignment keeps them lock-free in the i386 build too.
    class alignas(8) Statistics {
    public:
        uint64_t rxPackets;
        uint64_t rxBytes;
        uint64_t rxDropped;
        uint64_t rxErrors;
        uint64_t txPackets;
        uint64_t txBytes;
        uint64_t txDropped;
        uint64_t txErrors;
    };

    virtual ~NetDevice() {}
    NetDevice(const NetDevice &) = delete;
    NetDevice &operator=(const NetDevice &) = delete;

    const char *name() const { return nameBuffer; }
    virtual const char *driver() const = 0;
    virtual bool needsArp() const { return true; }
    virtual bool isLoopback() const { return false; }
    virtual bool hasLink() const { return true; }
    const MacAddress &mac() const { return address; }
    virtual uint16_t mtu() const { return ETHERNET_MTU; }
    bool isUp() const { return up; }
    const Statistics &statistics() const { return stats; }

    bool hasIpv4() const { return !ip.isZero(); }
    bool hasBroadcast() const { return !broadcast.isZero(); }
    const Ipv4Address &ipv4() const { return ip; }
    const Ipv4Address &netmask() const { return mask; }
    const Ipv4Address &broadcastAddress() const { return broadcast; }
    unsigned announcementsLeft() const { return announcements; }
    void setAnnouncements(unsigned count) { announcements = count; }
    void setIpv4(const Ipv4Address &address, const Ipv4Address &netmask, const Ipv4Address &broadcastAddress) {
        ip = address;
        mask = netmask;
        broadcast = broadcastAddress;
    }

    int open();
    void close();

    int transmit(PacketBuffer *frame);

    void receive(PacketBuffer *frame);
    void countRxDropped() { __atomic_fetch_add(&stats.rxDropped, 1, __ATOMIC_RELAXED); }
    void countRxError() { __atomic_fetch_add(&stats.rxErrors, 1, __ATOMIC_RELAXED); }

protected:
    NetDevice(const char *deviceName, const MacAddress &hardwareAddress);

    virtual int start() { return 0; }
    virtual void stop() {}
    virtual int send(PacketBuffer *frame) = 0;

    void setMac(const MacAddress &hardwareAddress) { address = hardwareAddress; }
    void countTxError() { __atomic_fetch_add(&stats.txErrors, 1, __ATOMIC_RELAXED); }

private:
    char nameBuffer[NAME_LENGTH];
    MacAddress address;
    Ipv4Address ip;
    Ipv4Address mask;
    Ipv4Address broadcast;
    unsigned announcements;
    bool up;
    Statistics stats;
};

#endif
