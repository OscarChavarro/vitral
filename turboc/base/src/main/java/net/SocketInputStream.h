#ifndef __SOCKET_INPUT_STREAM__
#define __SOCKET_INPUT_STREAM__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/InputStream.h"

class SocketInputStream : public InputStream {
    int fd_;
public:
    explicit SocketInputStream(int fd) : fd_(fd) {}
    int read();
    int read(unsigned char* buffer, int offset, int length);
    void close();
};


#endif
