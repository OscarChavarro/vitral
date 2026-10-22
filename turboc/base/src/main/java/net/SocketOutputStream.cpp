
#include "java/lang/RuntimeException.h"
#include "java/net/SocketOutputStream.h"
#include <sys/socket.h>
#include <unistd.h>

void SocketOutputStream::write(int value) {
    unsigned char b = ((unsigned char)(value & 0xFF));
    if (::send(fd_, &b, 1, 0) <= 0)
        throw RuntimeException("socket write error");
}

void SocketOutputStream::write(const unsigned char* buffer, int offset, int length) {
    const unsigned char* p = buffer + offset;
    int remaining = length;
    while (remaining > 0) {
        ssize_t sent = ::send(fd_, p, ((size_t)(remaining)), 0);
        if (sent <= 0)
            throw RuntimeException("socket write error");
        p += sent;
        remaining -= ((int)(sent));
    }
}

void SocketOutputStream::close() {
    ::close(fd_);
}

