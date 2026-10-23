#ifndef __SOCKET__
#define __SOCKET__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/net/SocketInputStream.h"
#include "java/net/SocketOutputStream.h"

class Socket {
    int fd_;
    SocketInputStream  in_;
    SocketOutputStream out_;
    BOOLEAN ownsSocket_;

    static int connectToHost(const String& host, int port);

public:
    Socket(const String& host, int port);
    explicit Socket(int fd) : fd_(fd), in_(fd), out_(fd), ownsSocket_(TRUE) {}
    ~Socket() { if (ownsSocket_) close(); }

    InputStream*  getInputStream()  { return &in_;  }
    OutputStream* getOutputStream() { return &out_; }
    BOOLEAN isConnected() const { return fd_ >= 0; }
    int fd() const { return fd_; }
    int releaseFd() { ownsSocket_ = FALSE; return fd_; }
    void close();
};


#endif
