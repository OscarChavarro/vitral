#ifndef __PACKET_QUEUE__
#define __PACKET_QUEUE__
#include "lib/vitralKernel/SpinLock.h"
#include "net/PacketBuffer.h"
#include "lib/vitralKernel/LockGuard.h"

class PacketQueue {
public:
    explicit constexpr PacketQueue(unsigned maxLength) : first(nullptr), last(nullptr), count(0), limit(maxLength) {}

    bool push(PacketBuffer *packet) {
        LockGuard guard(lock);
        if (count >= limit) {
            return false;
        }
        packet->next = nullptr;
        if (last) {
            last->next = packet;
        } else {
            first = packet;
        }
        last = packet;
        count++;
        return true;
    }
    PacketBuffer *pop() {
        LockGuard guard(lock);
        PacketBuffer *packet = first;
        if (!packet) {
            return nullptr;
        }
        first = packet->next;
        if (!first) {
            last = nullptr;
        }
        packet->next = nullptr;
        count--;
        return packet;
    }
    unsigned length() {
        LockGuard guard(lock);
        return count;
    }

private:
    SpinLock lock;
    PacketBuffer *first;
    PacketBuffer *last;
    unsigned count;
    unsigned limit;
};

#endif
