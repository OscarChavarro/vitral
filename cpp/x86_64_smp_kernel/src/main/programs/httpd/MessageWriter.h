#ifndef __MESSAGE_WRITER__
#define __MESSAGE_WRITER__
#include <stdint.h>
#include <stddef.h>
#include "programs/httpd/WebSocket.h"

class MessageWriter {
public:
    MessageWriter() : length(0) {}
    void reset(uint8_t kind) { length = 0; put(kind); }
    bool fits(size_t more) const { return length + more <= WebSocket::SEND_MAX; }
    void put(uint8_t value) { bytes[length++] = value; }
    void putVar(uint32_t value) {
        while (value >= 0x80) {
            put((uint8_t)(value | 0x80));
            value >>= 7;
        }
        put((uint8_t)value);
    }
    void put32(uint32_t value) {
        for (int i = 0; i < 4; i++) {
            put((uint8_t)(value >> (8 * i)));
        }
    }
    bool send(WebSocket &socket) { return socket.sendBinary(bytes, length); }
    size_t size() const { return length; }
    uint8_t *data() { return bytes; }
    static size_t varSize(uint32_t value) {
        size_t n = 1;
        while (value >= 0x80) { value >>= 7; n++; }
        return n;
    }

private:
    uint8_t bytes[WebSocket::SEND_MAX];
    size_t length;
};

#endif
