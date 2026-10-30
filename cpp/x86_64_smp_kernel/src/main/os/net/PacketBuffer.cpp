#include "net/PacketBuffer.h"
#include "kernel/KernelHeap.h"

static unsigned allocated = 0;

PacketBuffer *
PacketBuffer::allocate() {
    PacketBuffer *packet = (PacketBuffer *)KernelHeap::tryAllocate(sizeof(PacketBuffer));
    if (!packet) {
        return nullptr;
    }
    packet->device = nullptr;
    packet->next = nullptr;
    packet->head = packet->tail = HEADROOM;
    __atomic_fetch_add(&allocated, 1, __ATOMIC_RELAXED);
    return packet;
}

void
PacketBuffer::release(PacketBuffer *packet) {
    if (!packet) {
        return;
    }
    __atomic_fetch_sub(&allocated, 1, __ATOMIC_RELAXED);
    KernelHeap::release(packet);
}

unsigned
PacketBuffer::inUse() { return __atomic_load_n(&allocated, __ATOMIC_RELAXED); }

uint8_t *
PacketBuffer::put(size_t bytes) {
    if (bytes > tailroom()) {
        return nullptr;
    }
    uint8_t *area = storage + tail;
    tail += bytes;
    return area;
}

uint8_t *
PacketBuffer::push(size_t bytes) {
    if (bytes > head) {
        return nullptr;
    }
    head -= bytes;
    return storage + head;
}

uint8_t *
PacketBuffer::pull(size_t bytes) {
    if (bytes > length()) {
        return nullptr;
    }
    uint8_t *area = storage + head;
    head += bytes;
    return area;
}

bool
PacketBuffer::trim(size_t newLength) {
    if (newLength > length()) {
        return false;
    }
    tail = head + newLength;
    return true;
}
