#include "net/NetDevice.h"
#include "fs/Err.h"
#include "net/Ethernet.h"
#include "net/Net.h"
#include "net/NetDevices.h"
#include "lib/vitralKernel/String.h"

NetDevice::NetDevice(const char *deviceName, const MacAddress &hardwareAddress)
    : address(hardwareAddress), announcements(0), up(false), stats{} {
    String::copy(nameBuffer, deviceName, NAME_LENGTH);
}

int
NetDevice::open() {
    if (up) {
        return 0;
    }
    int status = start();
    if (status == 0) {
        up = true;
    }
    return status;
}

void
NetDevice::close() {
    if (!up) {
        return;
    }
    up = false;
    stop();
}

int
NetDevice::transmit(PacketBuffer *frame) {
    if (!up) {
        PacketBuffer::release(frame);
        return -Err::NODEV;
    }
    size_t size = frame->length();
    if (size < Ethernet::HEADER_SIZE || size > Ethernet::HEADER_SIZE + mtu()) {
        __atomic_fetch_add(&stats.txDropped, 1, __ATOMIC_RELAXED);
        PacketBuffer::release(frame);
        return -Err::INVAL;
    }
    frame->device = this;
    int status = send(frame);
    if (status < 0) {
        __atomic_fetch_add(&stats.txDropped, 1, __ATOMIC_RELAXED);
        return status;
    }
    __atomic_fetch_add(&stats.txPackets, 1, __ATOMIC_RELAXED);
    __atomic_fetch_add(&stats.txBytes, size, __ATOMIC_RELAXED);
    return 0;
}

void
NetDevice::receive(PacketBuffer *frame) {
    if (!up) {
        PacketBuffer::release(frame);
        return;
    }
    frame->device = this;
    size_t size = frame->length();
    if (!Net::enqueueReceived(frame)) {
        __atomic_fetch_add(&stats.rxDropped, 1, __ATOMIC_RELAXED);
        PacketBuffer::release(frame);
        return;
    }
    __atomic_fetch_add(&stats.rxPackets, 1, __ATOMIC_RELAXED);
    __atomic_fetch_add(&stats.rxBytes, size, __ATOMIC_RELAXED);
}
