#include "boot/BiosDisk.h"

uint8_t BiosDisk::drive = 0;
uint32_t BiosDisk::sectorsPerTrack = 18;
uint32_t BiosDisk::heads = 2;

bool
BiosDisk::init(uint8_t d) {
    drive = d;
    uint32_t ax = 0x0800;
    uint32_t cx = 0;
    uint32_t dx = d;
    bool carry;
    asm volatile("xorw %%di, %%di\n\t"
                 "int $0x13"
                 : "+a"(ax), "+c"(cx), "+d"(dx), "=@ccc"(carry) : : "ebx", "edi", "memory");
    if (carry || (cx & 0x3F) == 0) {
        return false;
    }
    sectorsPerTrack = cx & 0x3F;
    heads = ((dx >> 8) & 0xFF) + 1;
    return true;
}

uint32_t
BiosDisk::readToBounce(uint32_t lba, uint32_t count) {
    uint32_t sector = lba % sectorsPerTrack;
    uint32_t head = (lba / sectorsPerTrack) % heads;
    uint32_t cyl = lba / (sectorsPerTrack * heads);
    if (count > sectorsPerTrack - sector) {
        count = sectorsPerTrack - sector;
    }
    for (int attempt = 0; attempt < 4; attempt++) {
        uint32_t ax = 0x0200 | count;
        uint32_t cx = ((cyl & 0xFF) << 8) | ((cyl >> 2) & 0xC0) | (sector + 1);
        uint32_t dx = (head << 8) | drive;
        bool carry;
        asm volatile("pushw %%es\n\t"
                     "pushw %[seg]\n\t"
                     "popw %%es\n\t"
                     "xorl %%ebx, %%ebx\n\t"
                     "int $0x13\n\t"
                     "popw %%es"
                     : "+a"(ax), "+c"(cx), "+d"(dx), "=@ccc"(carry)
                     : [seg] "i"(BOUNCE_SEGMENT) : "ebx", "memory");
        if (!carry) {
            return count;
        }
        uint32_t reset = 0;
        asm volatile("int $0x13" : "+a"(reset) : "d"((uint32_t)drive) : "cc", "memory");
    }
    return 0;
}

uint32_t
BiosDisk::bounceRead32(uint16_t offset) {
    uint32_t v;
    asm volatile("pushw %%fs\n\t"
                 "pushw %[seg]\n\t"
                 "popw %%fs\n\t"
                 "movl %%fs:(%%ebx), %0\n\t"
                 "popw %%fs"
                 : "=&r"(v) : "b"((uint32_t)offset), [seg] "i"(BOUNCE_SEGMENT) : "memory");
    return v;
}
