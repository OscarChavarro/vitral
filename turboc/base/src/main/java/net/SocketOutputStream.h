#ifndef __SOCKET_OUTPUT_STREAM__
#define __SOCKET_OUTPUT_STREAM__

#include "java/io/OutputStream.h"

class SocketOutputStream : public OutputStream {
    int fd_;
public:
    explicit SocketOutputStream(int fd) : fd_(fd) {}
    void write(int value);
    void write(const unsigned char* buffer, int offset, int length);
    void flush() {}
    void close();
};


#endif
