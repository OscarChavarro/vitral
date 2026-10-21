#include <stdint.h>
#include <stdexcept>

#include "java/net/SocketOutputStream.h"
#include <sys/socket.h>
#include <unistd.h>

void SocketOutputStream::write(int value) {
    uint8_t b = ((uint8_t)(value & 0xFF));
    if (::send(fd_, &b, 1, 0) <= 0)
        throw std::runtime_error("socket write error");
}

void SocketOutputStream::write(const unsigned char* buffer, int offset, int length) {
    const unsigned char* p = buffer + offset;
    int remaining = length;
    while (remaining > 0) {
        ssize_t sent = ::send(fd_, p, ((size_t)(remaining)), 0);
        if (sent <= 0)
            throw std::runtime_error("socket write error");
        p += sent;
        remaining -= ((int)(sent));
    }
}

void SocketOutputStream::close() {
    ::close(fd_);
}

