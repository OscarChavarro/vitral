#ifndef JAVA_NET_SOCKET_H__
#define JAVA_NET_SOCKET_H__

#include "java/net/SocketInputStream.h"
#include "java/net/SocketOutputStream.h"
namespace java {
namespace net {

// Wraps a connected TCP socket file descriptor, mirroring java.net.Socket.
class Socket {
    int fd_;
    SocketInputStream  in_;
    SocketOutputStream out_;
    bool ownsSocket_;
public:
    explicit Socket(int fd) : fd_(fd), in_(fd), out_(fd), ownsSocket_(true) {}
    ~Socket() { if (ownsSocket_) close(); }

    java::InputStream*  getInputStream()  { return &in_;  }
    java::OutputStream* getOutputStream() { return &out_; }
    int fd() const { return fd_; }
    int releaseFd() { ownsSocket_ = false; return fd_; }
    void close();
};

} // net
} // java

#endif
