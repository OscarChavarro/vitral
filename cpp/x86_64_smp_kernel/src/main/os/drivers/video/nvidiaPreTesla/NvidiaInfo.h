#ifndef __NVIDIA_INFO__
#define __NVIDIA_INFO__
#include <stdint.h>

// What Syscalls::nvidia() reports of the pre-Tesla NVIDIA card (NvidiaPreTesla) and its 2D engine (NvidiaPreTesla2d),
// for the `nvidia` program. Frequencies are in kHz, sizes in bytes; a value the chip does not have is 0.
class NvidiaInfo {
public:
    static constexpr unsigned NAME_BYTES = 48;
    static constexpr unsigned TEXT_BYTES = 24;
    static constexpr unsigned MONITOR_NAME_BYTES = 14;

    // The operations of Syscalls::nvidia().
    static constexpr uint32_t DESCRIBE = 0;
    static constexpr uint32_t ACCELERATION_ON = 1;
    static constexpr uint32_t ACCELERATION_OFF = 2;
    static constexpr uint32_t TEST = 3;

    // What TEST did: a check of the 2D engine against the CPU in video memory the screen does not show, and the
    // speed of both.
    class Test {
    public:
        bool ran;
        bool passed;
        char failure[NAME_BYTES];
        uint32_t checkedPixels;
        uint32_t wrongPixels;
        uint32_t firstWrongX;
        uint32_t firstWrongY;
        uint32_t expected;
        uint32_t found;
        // Millions of pixels per second, times 10.
        uint32_t engineFill;
        uint32_t cpuFill;
        uint32_t engineCopy;
        uint32_t cpuCopy;
    };

    char chipName[NAME_BYTES];
    char familyName[NAME_BYTES];
    uint16_t pciVendor;
    uint16_t pciDevice;
    // The device ID nvidiafb keys its engine set-up on: the GPU's own one behind an AGP to PCI Express bridge.
    uint16_t implementation;
    uint16_t subsystemVendor;
    uint16_t subsystemDevice;
    uint8_t bus;
    uint8_t slot;
    uint8_t function;
    uint8_t revision;
    uint16_t chipset;
    uint8_t biosChip;
    char biosVersion[TEXT_BYTES];
    uint32_t boot0;
    uint32_t straps;
    uint64_t registersAddress;
    uint64_t registersBytes;
    uint64_t frameAddress;
    uint64_t frameBytes;
    uint32_t memoryBytes;
    uint32_t memoryBusBits;
    // nvidiafb's layout: what the 2D engine addresses (at most 64 MB), and what modes can use below its push buffer.
    uint32_t mapBytes;
    uint32_t usableBytes;
    uint32_t crystalKhz;
    uint32_t coreKhz;
    uint32_t memoryKhz;
    uint32_t maxPixelKhz;
    bool twoStagePll;
    uint32_t vco1MinKhz;
    uint32_t vco1MaxKhz;
    uint32_t vco2MinKhz;
    uint32_t vco2MaxKhz;

    bool monitor;
    char monitorVendor[4];
    uint16_t monitorProduct;
    char monitorName[MONITOR_NAME_BYTES];
    bool monitorAnalog;
    uint8_t ddcPort;
    uint16_t preferredWidth;
    uint16_t preferredHeight;
    uint16_t preferredHz;
    uint32_t preferredKhz;

    // The mode on the screen; width 0 in VGA modes.
    uint16_t width;
    uint16_t height;
    uint8_t bytesPerPixel;
    uint32_t pitch;
    uint32_t pixelKhz;
    uint32_t vpll;
    uint32_t vpllB;

    // Svga's primitives (framebuffer or the 2D engine's) and the 2D engine's state.
    char primitives[TEXT_BYTES];
    bool accelerationSelected;
    bool engineStarted;
    bool engineLockedUp;
    uint32_t pushBufferOffset;
    uint32_t pushBufferBytes;
    uint64_t fills;
    uint64_t copies;
    uint64_t kickoffs;
    uint32_t lockups;
    uint32_t graphStatus;
    uint32_t fifoGet;
    uint32_t fifoPut;

    Test test;
};

#endif
