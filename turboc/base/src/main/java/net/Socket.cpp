#include "vsdk/toolkit/common/VSDK.h"
#include "java/net/Socket.h"
#include <stdio.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>

int Socket::connectToHost(const String& host, int port)
{
    // gethostbyname instead of getaddrinfo, which old C libraries (i.e.
    // libc 5 on Debian 1.1) do not have: IPv4 only
    struct hostent* hostEntry = gethostbyname(host.c_str());
    if ( hostEntry == 0 || hostEntry->h_addrtype != AF_INET ) {
        return -1;
    }

    int fd = -1;
    for ( int i = 0; hostEntry->h_addr_list[i] != 0; i++ ) {
        struct sockaddr_in address;
        memset(&address, 0, sizeof(address));
        address.sin_family = AF_INET;
        address.sin_port = htons((unsigned short)port);
        memcpy(&address.sin_addr, hostEntry->h_addr_list[i],
            hostEntry->h_length);

        fd = ::socket(AF_INET, SOCK_STREAM, 0);
        if ( fd < 0 ) {
            continue;
        }
        if ( ::connect(fd, (struct sockaddr*)&address, sizeof(address)) == 0 ) {
            break;
        }
        ::close(fd);
        fd = -1;
    }
    return fd;
}

Socket::Socket(const String& host, int port)
    : fd_(connectToHost(host, port)), in_(fd_), out_(fd_), ownsSocket_(TRUE)
{
}

void Socket::close() {
    if (fd_ >= 0) {
        ::shutdown(fd_, 2); // SHUT_RDWR
        ::close(fd_);
        fd_ = -1;
    }
}

