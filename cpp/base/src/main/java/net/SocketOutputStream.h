#ifndef JAVA_NET_SOCKET_OUTPUT_STREAM_H__
#define JAVA_NET_SOCKET_OUTPUT_STREAM_H__

#include "java/io/OutputStream.h"
namespace java {
namespace net {

class SocketOutputStream : public java::OutputStream {
    int fd_;
public:
    explicit SocketOutputStream(int fd) : fd_(fd) {}
    void write(int value) override;
    void write(const unsigned char* buffer, int offset, int length) override;
    void flush() override {}
    void close() override;
};

} // net
} // java

#endif
