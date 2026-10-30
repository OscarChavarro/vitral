#ifndef __HTTPD_WEB_SOCKET__
#define __HTTPD_WEB_SOCKET__
#include "syscalls/Syscalls.h"

class WebSocket {
public:
    static constexpr size_t ACCEPT_SIZE = 29;
    static constexpr size_t PAYLOAD_MAX = 125;
    static constexpr size_t SEND_MAX = 480;

    enum class Event { Message, Timeout, Closed };

    explicit WebSocket(int64_t socket) : socket(socket), buffered(0) {}

    static void acceptValue(const char *key, char out[ACCEPT_SIZE]);

    bool sendBinary(const void *payload, size_t length);
    bool sendPing();
    Event poll(uint32_t timeoutMs, uint8_t *message, size_t capacity, size_t &length);

private:
    bool sendFrame(uint8_t opcode, const void *payload, size_t length);
    int64_t socket;
    uint8_t pending[2 + 4 + PAYLOAD_MAX];
    size_t buffered;
};

#endif
