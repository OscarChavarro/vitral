#ifndef WEBSOCKET_PROTOCOL_HPP
#define WEBSOCKET_PROTOCOL_HPP

#include <cstdint>
#include <cstddef>

#include "java/util/ArrayList.h"
#include "java/lang/String.h"
#include "java/io/InputStream.h"
#include "java/io/OutputStream.h"
#include "webservice/Protocol.hpp"

class WebSocketProtocol {
public:
    WebSocketProtocol();

    bool performHandshake(java::InputStream* in, java::OutputStream* out,
                         const char* requiredPath);

    bool sendJsonMessage(java::OutputStream* out, const java::ArrayList<MarkerGroupPose>& groups);

    void sendCloseFrame(java::OutputStream* out);

private:
    struct Sha1 {
        uint32_t h[5];
        uint64_t len;
        uint8_t buf[64];
        size_t bufLen;
        Sha1() { reset(); }
        void reset();
        static uint32_t rol(uint32_t v, int b);
        void block(const uint8_t* p);
        void update(const uint8_t* p, size_t n);
        void finish(uint8_t out[20]);
    };

    java::String readHttpRequest(java::InputStream* in);
    java::String base64Encode(const uint8_t* data, size_t size);
    void writeAll(java::OutputStream* out, const java::String& s);
    void writeAll(java::OutputStream* out, const java::ArrayList<uint8_t>& v);
};

#endif
