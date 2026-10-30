#include "programs/httpd/WebSocket.h"
#include "lib/vitralKernel/Memory.h"
#include "programs/httpd/WsCrypto.h"

void
WebSocket::acceptValue(const char *key, char out[ACCEPT_SIZE]) {
    static const char GUID[] = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    uint8_t input[WsCrypto::SHA1_INPUT_MAX];
    size_t n = 0;
    while (key[n] && n < 64) {
        input[n] = (uint8_t)key[n];
        n++;
    }
    for (size_t i = 0; GUID[i]; i++) {
        input[n++] = (uint8_t)GUID[i];
    }
    uint8_t digest[20];
    WsCrypto::sha1(input, n, digest);
    WsCrypto::base64Of20(digest, out);
}

bool
WebSocket::sendFrame(uint8_t opcode, const void *payload, size_t length) {
    uint8_t frame[4 + SEND_MAX];
    if (length > SEND_MAX) {
        return false;
    }
    size_t n = 0;
    frame[n++] = 0x80 | opcode;
    if (length < 126) {
        frame[n++] = (uint8_t)length;
    } else {
        frame[n++] = 126;
        frame[n++] = (uint8_t)(length >> 8);
        frame[n++] = (uint8_t)length;
    }
    Memory::copy(frame + n, payload, length);
    return Syscalls::tcpSend(socket, frame, n + length) >= 0;
}

bool
WebSocket::sendBinary(const void *payload, size_t length) { return sendFrame(0x2, payload, length); }
bool
WebSocket::sendPing() { return sendFrame(0x9, nullptr, 0); }

WebSocket::Event
WebSocket::poll(uint32_t timeoutMs, uint8_t *message, size_t capacity, size_t &length) {
    uint64_t deadline = Syscalls::uptimeMs() + (timeoutMs ? timeoutMs : 1);
    for (;;) {
        if (buffered >= 2) {
            size_t payload = pending[1] & 0x7F;
            bool masked = pending[1] & 0x80;
            size_t header = 2 + (masked ? 4 : 0);
            if (payload > PAYLOAD_MAX || !masked) {
                sendFrame(0x8, "\x03\xF1", 2);
                return Event::Closed;
            }
            if (buffered >= header + payload) {
                uint8_t opcode = pending[0] & 0x0F;
                bool fin = pending[0] & 0x80;
                uint8_t data[PAYLOAD_MAX];
                for (size_t i = 0; i < payload; i++) {
                    data[i] = pending[header + i] ^ pending[2 + (i & 3)];
                }
                size_t used = header + payload;
                for (size_t i = 0; i < buffered - used; i++) {
                    pending[i] = pending[used + i];
                }
                buffered -= used;
                if (opcode == 0x8) {
                    sendFrame(0x8, data, payload >= 2 ? 2 : 0);
                    return Event::Closed;
                }
                if (opcode == 0x9) {
                    if (!sendFrame(0xA, data, payload)) {
                        return Event::Closed;
                    }
                    continue;
                }
                if (opcode == 0xA) {
                    continue;
                }
                if (!fin || opcode == 0x0) {
                    sendFrame(0x8, "\x03\xEF", 2);
                    return Event::Closed;
                }
                length = payload < capacity ? payload : capacity;
                Memory::copy(message, data, length);
                return Event::Message;
            }
        }
        uint64_t now = Syscalls::uptimeMs();
        if (now >= deadline) {
            return Event::Timeout;
        }
        int64_t n = Syscalls::tcpReceive(socket, pending + buffered, sizeof(pending) - buffered, (uint32_t)(deadline - now));
        if (n == -Syscalls::ERR_TIMEDOUT) {
            return Event::Timeout;
        }
        if (n <= 0) {
            return Event::Closed;
        }
        buffered += (size_t)n;
    }
}
