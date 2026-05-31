#include "java/net/SocketOutputStream.h"
#include <unistd.h>
#include <sys/socket.h>
#include <cstdint>

namespace java {
namespace net {

void SocketOutputStream::write(int value) {
    uint8_t b = static_cast<uint8_t>(value & 0xFF);
    ::send(fd_, &b, 1, 0);
}

void SocketOutputStream::write(const unsigned char* buffer, int offset, int length) {
    const unsigned char* p = buffer + offset;
    int remaining = length;
    while (remaining > 0) {
        ssize_t sent = ::send(fd_, p, static_cast<size_t>(remaining), 0);
        if (sent <= 0) return;
        p += sent;
        remaining -= static_cast<int>(sent);
    }
}

void SocketOutputStream::close() {
    ::close(fd_);
}

} // net
} // java
