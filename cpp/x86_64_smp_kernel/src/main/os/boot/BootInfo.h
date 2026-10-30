#ifndef __BOOT_INFO__
#define __BOOT_INFO__
#include <stddef.h>
#include <stdint.h>
#include "boot/E820Entry.h"

class BootInfo {
public:
    static constexpr uint32_t ADDRESS = 0x1000;
    static constexpr uint32_t MAGIC = 0x544F4F42;
    static constexpr unsigned MAX_E820 = 160;
    static constexpr uint32_t KERNEL_BASE = 0x100000;

    uint32_t magic;
    uint32_t bootDrive;
    uint32_t a20Enabled;
    uint32_t e820Count;
    uint32_t kernelBase;
    uint32_t kernelImageBytes;
    uint32_t kernelEntry;
    uint32_t kernelEnd;
    uint64_t frameBufferAddress;
    uint32_t frameBufferPitch;
    uint32_t frameBufferWidth;
    uint32_t frameBufferHeight;
    uint8_t frameBufferBytes;
    uint8_t frameBufferType;
    uint8_t frameBufferRedPosition;
    uint8_t frameBufferRedSize;
    uint8_t frameBufferGreenPosition;
    uint8_t frameBufferGreenSize;
    uint8_t frameBufferBluePosition;
    uint8_t frameBufferBlueSize;
    alignas(8) E820Entry e820[MAX_E820];

    static BootInfo &get() { return *(BootInfo *)(uintptr_t)ADDRESS; }
};

static_assert(offsetof(BootInfo, e820) == 64, "BootInfo E820 offset must match in 16-bit and 64-bit code");
static_assert(sizeof(E820Entry) == 24, "E820 entries must match the BIOS layout");

#endif
