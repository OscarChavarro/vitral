#include "java/net/Socket.h"
#include <unistd.h>
namespace java {
namespace net {

void Socket::close() {
    if (fd_ >= 0) {
        ::close(fd_);
        fd_ = -1;
    }
}

} // net
} // java
