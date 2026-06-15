#include "webservice/WebSocketProtocol.hpp"
#include "webservice/Protocol.hpp"
#include "java/util/ArrayList.txx"

#include <cstdio>
#include <cstring>
#include <cstddef>

WebSocketProtocol::WebSocketProtocol() {}

uint32_t WebSocketProtocol::Sha1::rol(uint32_t v, int b) {
    return (v << b) | (v >> (32 - b));
}

void WebSocketProtocol::Sha1::reset() {
    h[0] = 0x67452301u;
    h[1] = 0xEFCDAB89u;
    h[2] = 0x98BADCFEu;
    h[3] = 0x10325476u;
    h[4] = 0xC3D2E1F0u;
    len = 0;
    bufLen = 0;
}

void WebSocketProtocol::Sha1::block(const uint8_t* p) {
    uint32_t w[80];
    for (int i = 0; i < 16; ++i)
        w[i] = (p[i*4] << 24) | (p[i*4+1] << 16) | (p[i*4+2] << 8) | p[i*4+3];
    for (int i = 16; i < 80; ++i)
        w[i] = rol(w[i-3] ^ w[i-8] ^ w[i-14] ^ w[i-16], 1);
    uint32_t a = h[0], b = h[1], c = h[2], d = h[3], e = h[4];
    for (int i = 0; i < 80; ++i) {
        uint32_t f, k;
        if (i < 20) {
            f = (b & c) | ((~b) & d);
            k = 0x5A827999u;
        } else if (i < 40) {
            f = b ^ c ^ d;
            k = 0x6ED9EBA1u;
        } else if (i < 60) {
            f = (b & c) | (b & d) | (c & d);
            k = 0x8F1BBCDCu;
        } else {
            f = b ^ c ^ d;
            k = 0xCA62C1D6u;
        }
        uint32_t t = rol(a, 5) + f + e + k + w[i];
        e = d;
        d = c;
        c = rol(b, 30);
        b = a;
        a = t;
    }
    h[0] += a;
    h[1] += b;
    h[2] += c;
    h[3] += d;
    h[4] += e;
}

void WebSocketProtocol::Sha1::update(const uint8_t* p, size_t n) {
    len += n;
    while (n) {
        size_t take = 64 - bufLen;
        if (take > n) take = n;
        std::memcpy(buf + bufLen, p, take);
        bufLen += take;
        p += take;
        n -= take;
        if (bufLen == 64) {
            block(buf);
            bufLen = 0;
        }
    }
}

void WebSocketProtocol::Sha1::finish(uint8_t out[20]) {
    uint64_t bits = len * 8;
    uint8_t pad = 0x80;
    update(&pad, 1);
    uint8_t z = 0;
    while (bufLen != 56)
        update(&z, 1);
    uint8_t lb[8];
    for (int i = 0; i < 8; ++i)
        lb[i] = (uint8_t)(bits >> (56 - 8 * i));
    update(lb, 8);
    for (int i = 0; i < 5; ++i) {
        out[i*4]     = (uint8_t)(h[i] >> 24);
        out[i*4 + 1] = (uint8_t)(h[i] >> 16);
        out[i*4 + 2] = (uint8_t)(h[i] >> 8);
        out[i*4 + 3] = (uint8_t)(h[i]);
    }
}

java::String WebSocketProtocol::base64Encode(const uint8_t* data, size_t size) {
    static const char* alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    char buffer[1024];
    int bufPos = 0;
    for (size_t i = 0; i < size && bufPos + 4 < 1024; i += 3) {
        uint32_t value = data[i] << 16;
        if (i + 1 < size) value |= data[i + 1] << 8;
        if (i + 2 < size) value |= data[i + 2];
        buffer[bufPos++] = alphabet[(value >> 18) & 63];
        buffer[bufPos++] = alphabet[(value >> 12) & 63];
        buffer[bufPos++] = (i + 1 < size) ? alphabet[(value >> 6) & 63] : '=';
        buffer[bufPos++] = (i + 2 < size) ? alphabet[value & 63] : '=';
    }
    buffer[bufPos] = '\0';
    return java::String(buffer);
}

java::String WebSocketProtocol::readHttpRequest(java::InputStream* in) {
    char buffer[8192];
    int n = 0;
    while (n < 8191) {
        int c = in->read();
        if (c < 0) break;
        buffer[n++] = static_cast<char>(c);
        if (n >= 4 &&
            buffer[n-4] == '\r' && buffer[n-3] == '\n' &&
            buffer[n-2] == '\r' && buffer[n-1] == '\n') {
            break;
        }
    }
    buffer[n] = '\0';
    return java::String(buffer);
}

void WebSocketProtocol::writeAll(java::OutputStream* out, const java::String& s) {
    const char* data = s.c_str();
    for (int i = 0; i < s.length(); ++i) {
        out->write(static_cast<uint8_t>(data[i]));
    }
}

void WebSocketProtocol::writeAll(java::OutputStream* out, const java::ArrayList<uint8_t>& v) {
    if (v.size() > 0)
        out->write(const_cast<java::ArrayList<uint8_t>&>(v).data(), 0, static_cast<int>(v.size()));
}

bool WebSocketProtocol::performHandshake(java::InputStream* in, java::OutputStream* out,
                                          const char* requiredPath) {
    java::String request = readHttpRequest(in);
    const char* req = request.c_str();
    int reqLen = request.length();

    if (reqLen < 4 || req[0] != 'G' || req[1] != 'E' || req[2] != 'T' || req[3] != ' ') {
        writeAll(out, java::String("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n"));
        return false;
    }

    int sp = request.find(' ', 4);
    if (sp == java::String::npos) {
        writeAll(out, java::String("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n"));
        return false;
    }
    java::String path = request.substr(4, sp - 4);

    if (requiredPath && requiredPath[0] != '\0') {
        bool matches = true;
        for (int i = 0; requiredPath[i] && i < path.length(); ++i) {
            if (path[i] != requiredPath[i]) {
                matches = false;
                break;
            }
        }
        if (!matches) {
            writeAll(out, java::String("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"));
            return false;
        }
    }

    int keyPos = -1;
    const char* keyStr = "sec-websocket-key:";
    int keyLen = std::strlen(keyStr);
    for (int i = 0; i + keyLen <= reqLen; ++i) {
        bool match = true;
        for (int j = 0; j < keyLen; ++j) {
            char c1 = (req[i+j] >= 'A' && req[i+j] <= 'Z') ? req[i+j] - 'A' + 'a' : req[i+j];
            if (c1 != keyStr[j]) {
                match = false;
                break;
            }
        }
        if (match) {
            keyPos = i + keyLen;
            break;
        }
    }
    if (keyPos == -1) {
        writeAll(out, java::String("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n"));
        return false;
    }

    while (keyPos < reqLen && (req[keyPos] == ' ' || req[keyPos] == '\t'))
        ++keyPos;

    int keyEnd = -1;
    for (int i = keyPos; i + 1 < reqLen; ++i) {
        if (req[i] == '\r' && req[i+1] == '\n') {
            keyEnd = i;
            break;
        }
    }
    if (keyEnd == -1) {
        writeAll(out, java::String("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n"));
        return false;
    }

    java::String key = request.substr(keyPos, keyEnd - keyPos);
    java::String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    Sha1 sha;
    uint8_t digest[20];
    const char* magicData = magic.c_str();
    sha.update(reinterpret_cast<const uint8_t*>(magicData), magic.length());
    sha.finish(digest);
    java::String accept = base64Encode(digest, 20);

    java::String response = java::String("HTTP/1.1 101 Switching Protocols\r\n")
        + "Upgrade: websocket\r\n"
        + "Connection: Upgrade\r\n"
        + "Sec-WebSocket-Accept: ";
    response = response + accept;
    response = response + "\r\n\r\n";

    writeAll(out, response);
    out->flush();
    return true;
}

bool WebSocketProtocol::sendJsonMessage(java::OutputStream* out, const java::ArrayList<MarkerGroupPose>& groups) {
    java::ArrayList<uint8_t> payload;

    payload.add('[');
    for (long i = 0; i < groups.size(); ++i) {
        if (i > 0) payload.add(',');
        const MarkerGroupPose& p = groups.get(i);
        char buf[320];
        Quaterniond q = p.rotation.normalized();
        int len = std::snprintf(buf, sizeof(buf),
            "{\"label\":\"%s\",\"position\":[%.4f,%.4f,%.4f],\"quaternion\":[%.4f,%.4f,%.4f,%.4f]}",
            p.label.c_str(), p.position.x(), p.position.y(), p.position.z(),
            q.magnitude(), q.direction().x(), q.direction().y(), q.direction().z());
        for (int j = 0; j < len && j < 320; ++j) payload.add(static_cast<uint8_t>(buf[j]));
    }
    payload.add(']');

    java::ArrayList<uint8_t> frame;
    frame.add(0x81);
    size_t n = payload.size();
    if (n < 126) {
        frame.add(static_cast<uint8_t>(n));
    } else if (n <= 0xFFFF) {
        frame.add(126);
        frame.add(static_cast<uint8_t>(n >> 8));
        frame.add(static_cast<uint8_t>(n));
    } else {
        frame.add(127);
        for (int i = 7; i >= 0; --i) frame.add(static_cast<uint8_t>(n >> (8 * i)));
    }
    try {
        writeAll(out, frame);
        writeAll(out, payload);
        out->flush();
    } catch (...) {
        return false;
    }
    return true;
}

void WebSocketProtocol::sendCloseFrame(java::OutputStream* out) {
    java::ArrayList<uint8_t> closeFrame;
    closeFrame.add(0x88);
    closeFrame.add(0x00);
    try {
        writeAll(out, closeFrame);
        out->flush();
    } catch (...) {}
}
