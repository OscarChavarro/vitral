#ifndef __PACKET_BUFFER__
#define __PACKET_BUFFER__
#include <stdint.h>
#include <stddef.h>

class NetDevice;

class PacketBuffer {
public:
    static constexpr size_t STORAGE = 2048;
    static constexpr size_t HEADROOM = 144;

    static PacketBuffer *allocate();
    static void release(PacketBuffer *packet);
    static unsigned inUse();

    uint8_t *data() { return storage + head; }
    const uint8_t *data() const { return storage + head; }
    size_t length() const { return tail - head; }
    size_t headroom() const { return head; }
    size_t tailroom() const { return STORAGE - tail; }

    uint8_t *put(size_t bytes);
    uint8_t *push(size_t bytes);
    uint8_t *pull(size_t bytes);
    bool trim(size_t newLength);

    template <class T> T *as() { return (T *)data(); }

    NetDevice *device;
    uint8_t senderMac[6];
    PacketBuffer *next;

private:
    PacketBuffer() = delete;
    size_t head;
    size_t tail;
    uint8_t storage[STORAGE] __attribute__((aligned(16)));
};

#endif
