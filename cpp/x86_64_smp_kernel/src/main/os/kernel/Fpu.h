#ifndef __FPU__
#define __FPU__
#include <stdint.h>

// x87/SSE state of a process. The image is big enough for FXSAVE (512 bytes, 16-byte aligned) and for the older
// FSAVE (108 bytes) the i386 build uses on CPUs without FXSR. The instructions are in kernel/<arch>/Fpu.cpp.
class Fpu {
public:
    static constexpr unsigned IMAGE_SIZE = 512;

    class alignas(16) Image {
    public:
        uint8_t bytes[IMAGE_SIZE];
    };

    static void enable();
    static void initialize(Image &image);
    static void save(Image &image);
    static void restore(const Image &image);
};

#endif
