#ifndef __MULTIBOOT2__
#define __MULTIBOOT2__
#include <stdint.h>

class Multiboot2 {
public:
    static void fillBootInfo(uint32_t info);

private:
    static constexpr uint32_t TAG_END = 0;
    static constexpr uint32_t TAG_MEMORY_MAP = 6;
    static constexpr uint32_t TAG_FRAMEBUFFER = 8;
    static uint32_t read32(const uint8_t *p) { return *(const uint32_t *)p; }
    static uint64_t read64(const uint8_t *p) { return *(const uint64_t *)p; }
};

#endif
