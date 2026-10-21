#ifndef __SOCKET__
#define __SOCKET__

#include "java/lang/String.h"
#include "java/net/SocketInputStream.h"
#include "java/net/SocketOutputStream.h"

class Socket {
    int fd_;
    SocketInputStream  in_;
    SocketOutputStream out_;
    bool ownsSocket_;

    static int connectToHost(const String& host, int port);

public:
    Socket(const String& host, int port);
    explicit Socket(int fd) : fd_(fd), in_(fd), out_(fd), ownsSocket_(true) {}
    ~Socket() { if (ownsSocket_) close(); }

    InputStream*  getInputStream()  { return &in_;  }
    OutputStream* getOutputStream() { return &out_; }
    bool isConnected() const { return fd_ >= 0; }
    int fd() const { return fd_; }
    int releaseFd() { ownsSocket_ = false; return fd_; }
    void close();
};


#endif
