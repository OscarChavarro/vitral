#ifndef __WS_CRYPTO__
#define __WS_CRYPTO__
#include <stdint.h>
#include <stddef.h>
#include "lib/vitralKernel/Memory.h"

class WsCrypto {
public:
    static constexpr size_t SHA1_INPUT_MAX = 119;

    static void sha1(const uint8_t *data, size_t length, uint8_t out[20]) {
        uint8_t message[128];
        size_t total = length < 56 ? 64 : 128;
        Memory::set(message, 0, sizeof(message));
        Memory::copy(message, data, length);
        message[length] = 0x80;
        uint64_t bits = (uint64_t)length * 8;
        for (int i = 0; i < 8; i++) {
            message[total - 1 - i] = (uint8_t)(bits >> (8 * i));
        }
        uint32_t h[5] = {0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0};
        for (size_t offset = 0; offset < total; offset += 64) {
            uint32_t w[80];
            for (int i = 0; i < 16; i++) {
                w[i] = (uint32_t)message[offset + i * 4] << 24 | (uint32_t)message[offset + i * 4 + 1] << 16 |
                       (uint32_t)message[offset + i * 4 + 2] << 8 | message[offset + i * 4 + 3];
            }
            for (int i = 16; i < 80; i++) {
                w[i] = rotateLeft(w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16], 1);
            }
            uint32_t a = h[0];
            uint32_t b = h[1];
            uint32_t c = h[2];
            uint32_t d = h[3];
            uint32_t e = h[4];
            for (int i = 0; i < 80; i++) {
                uint32_t f;
                uint32_t k;
                if (i < 20) { f = (b & c) | (~b & d); k = 0x5A827999; }
                else if (i < 40) { f = b ^ c ^ d; k = 0x6ED9EBA1; }
                else if (i < 60) { f = (b & c) | (b & d) | (c & d); k = 0x8F1BBCDC; }
                else { f = b ^ c ^ d; k = 0xCA62C1D6; }
                uint32_t t = rotateLeft(a, 5) + f + e + k + w[i];
                e = d; d = c; c = rotateLeft(b, 30); b = a; a = t;
            }
            h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e;
        }
        for (int i = 0; i < 20; i++) {
            out[i] = (uint8_t)(h[i / 4] >> (24 - 8 * (i % 4)));
        }
    }

    static void base64Of20(const uint8_t in[20], char out[29]) {
        static const char ALPHABET[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        size_t n = 0;
        for (size_t i = 0; i < 20; i += 3) {
            uint32_t chunk = (uint32_t)in[i] << 16 | (i + 1 < 20 ? (uint32_t)in[i + 1] << 8 : 0) | (i + 2 < 20 ? in[i + 2] : 0);
            out[n++] = ALPHABET[chunk >> 18 & 63];
            out[n++] = ALPHABET[chunk >> 12 & 63];
            out[n++] = i + 1 < 20 ? ALPHABET[chunk >> 6 & 63] : '=';
            out[n++] = i + 2 < 20 ? ALPHABET[chunk & 63] : '=';
        }
        out[n] = 0;
    }

private:
    static uint32_t rotateLeft(uint32_t value, int bits) { return value << bits | value >> (32 - bits); }
};

#endif
