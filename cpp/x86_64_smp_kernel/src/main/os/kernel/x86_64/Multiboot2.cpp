#include "kernel/Multiboot2.h"
#include "boot/BootInfo.h"
#include "lib/vitralKernel/Memory.h"
#include "boot/E820Entry.h"

extern const char imageSizeSymbol[] asm("__image_size");
extern const char kernelEndSymbol[] asm("__kernel_end");
extern const char entrySymbol[] asm("_start64");

void
Multiboot2::fillBootInfo(uint32_t info) {
    BootInfo &boot = BootInfo::get();
    Memory::set(&boot, 0, sizeof(boot));
    boot.magic = BootInfo::MAGIC;
    boot.bootDrive = 0;
    boot.a20Enabled = 1;
    boot.kernelBase = BootInfo::KERNEL_BASE;
    boot.kernelImageBytes = (uint32_t)(uintptr_t)imageSizeSymbol;
    boot.kernelEntry = (uint32_t)(uintptr_t)entrySymbol;
    boot.kernelEnd = (uint32_t)(uintptr_t)kernelEndSymbol;

    const uint8_t *base = (const uint8_t *)(uintptr_t)info;
    uint32_t total = read32(base);
    for (uint32_t offset = 8; offset + 8 <= total;) {
        const uint8_t *tag = base + offset;
        uint32_t type = read32(tag);
        uint32_t size = read32(tag + 4);
        if (type == TAG_END || size < 8) {
            break;
        }
        if (type == TAG_MEMORY_MAP) {
            uint32_t entrySize = read32(tag + 8);
            for (uint32_t e = 16; e + entrySize <= size && entrySize >= 24; e += entrySize) {
                const uint8_t *entry = tag + e;
                uint64_t start = read64(entry);
                uint64_t length = read64(entry + 8);
                uint32_t kind = read32(entry + 16);
                if (length == 0) {
                    continue;
                }
                if (kind != 1 && kind != 3 && kind != 4 && kind != 5) {
                    kind = 2;
                }
                if (boot.e820Count) {
                    E820Entry &last = boot.e820[boot.e820Count - 1];
                    if (last.type == kind && last.base + last.length == start) {
                        last.length += length;
                        continue;
                    }
                }
                if (boot.e820Count == BootInfo::MAX_E820) {
                    break;
                }
                boot.e820[boot.e820Count++] = {start, length, kind, 1};
            }
        } else if (type == TAG_FRAMEBUFFER && size >= 32) {
            boot.frameBufferAddress = read64(tag + 8);
            boot.frameBufferPitch = read32(tag + 16);
            boot.frameBufferWidth = read32(tag + 20);
            boot.frameBufferHeight = read32(tag + 24);
            boot.frameBufferBytes = (uint8_t)((tag[28] + 7) / 8);
            boot.frameBufferType = tag[29];
            if (boot.frameBufferType == 1 && size >= 38) {
                boot.frameBufferRedPosition = tag[32];
                boot.frameBufferRedSize = tag[33];
                boot.frameBufferGreenPosition = tag[34];
                boot.frameBufferGreenSize = tag[35];
                boot.frameBufferBluePosition = tag[36];
                boot.frameBufferBlueSize = tag[37];
            }
        }
        offset += (size + 7) & ~7u;
    }
}
