
#include "java/net/SocketInputStream.h"
#include <sys/socket.h>
#include <unistd.h>

int SocketInputStream::read() {
    unsigned char b;
    ssize_t n = ::recv(fd_, &b, 1, 0);
    return (n <= 0) ? -1 : ((int)(b));
}

int SocketInputStream::read(unsigned char* buffer, int offset, int length) {
    ssize_t n = ::recv(fd_, buffer + offset, ((size_t)(length)), 0);
    return (n <= 0) ? -1 : ((int)(n));
}

void SocketInputStream::close() {
    ::close(fd_);
}

