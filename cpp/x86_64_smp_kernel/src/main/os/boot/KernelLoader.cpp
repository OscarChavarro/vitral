#include "boot/KernelLoader.h"
#include "boot/BiosDisk.h"
#include "lib/vitralKernel/Printf.h"
#include "boot/KernelHeader.h"

extern const char kernelLbaSymbol[] asm("__kernel_lba");

bool
KernelLoader::load(BootInfo &info) {
    uint32_t lba = (uint32_t)(uintptr_t)kernelLbaSymbol;
    if (!BiosDisk::init((uint8_t)info.bootDrive)) {
        return false;
    }

    if (BiosDisk::readToBounce(lba, 1) != 1) {
        return false;
    }
    KernelHeader header;
    header.magic = BiosDisk::bounceRead32(0);
    header.imageBytes = BiosDisk::bounceRead32(4);
    header.entry = BiosDisk::bounceRead32(8);
    header.bssEnd = BiosDisk::bounceRead32(12);
    if (header.magic != KernelHeader::MAGIC) {
        return false;
    }

    uint32_t total = (header.imageBytes + 511) / 512;
    uint32_t done = 0;
    uint32_t dest = BootInfo::KERNEL_BASE;
    while (done < total) {
        uint32_t got = BiosDisk::readToBounce(lba + done, total - done);
        if (got == 0) {
            return false;
        }
        copyHigh(dest, got * 512);
        dest += got * 512;
        done += got;
        printf16(".");
    }
    info.kernelBase = BootInfo::KERNEL_BASE;
    info.kernelImageBytes = header.imageBytes;
    info.kernelEntry = header.entry;
    info.kernelEnd = header.bssEnd;
    return true;
}
