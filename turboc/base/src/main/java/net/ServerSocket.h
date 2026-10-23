#ifndef __SERVER_SOCKET__
#define __SERVER_SOCKET__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/net/Socket.h"

// Mirrors java.net.ServerSocket: binds a port, accepts client connections.
class ServerSocket {
    int listenFd_;
    int port_;
public:
    explicit ServerSocket(int port);
    ~ServerSocket() { close(); }

    // Blocks until a client connects. Caller owns the returned Socket*.
    Socket* accept();
    void close();
    BOOLEAN isOpen() const { return listenFd_ >= 0; }
    int port() const { return port_; }
};


#endif
