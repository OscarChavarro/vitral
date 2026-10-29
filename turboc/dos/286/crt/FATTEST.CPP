/* Host-only test: ATA sectors live in RAM, no guest disk is touched.
 * g++ -x c++ -std=c++98 -Dfar= FATTEST.CPP ../Protected286Fat.cpp */
#include "../Protected286Ata.h"
#include "../Protected286Fat.h"
#include <assert.h>
#include <string.h>

static unsigned char disk[8192][512];
static unsigned sectorReads;
static void put16(unsigned at, unsigned value)
{
    disk[0][at] = (unsigned char)value;
    disk[0][at + 1] = (unsigned char)(value >> 8);
}
int Protected286Ata::readSector(Protected286Dword lba, Protected286Word* buffer)
{
    if (lba >= 8192) return 0;
    ++sectorReads;
    memcpy(buffer, disk[lba], 512);
    return 1;
}
int Protected286Ata::writeSector(Protected286Dword lba, const Protected286Word* buffer)
{
    if (lba >= 8192) return 0;
    memcpy(disk[lba], buffer, 512);
    return 1;
}
int main()
{
    Protected286Fat fat;
    char name[] = "FILE0.DAT";
    unsigned char source[1400], result[1400];
    int h;
    unsigned i;
    disk[0][0] = 0xEB;
    put16(11, 512);
    disk[0][13] = 1;
    put16(14, 1);
    disk[0][16] = 2;
    put16(17, 32);
    put16(19, 4096);
    put16(22, 12);
    disk[0][510] = 0x55; disk[0][511] = 0xAA;
    disk[1][0] = disk[13][0] = 0xF8;
    disk[1][1] = disk[1][2] = disk[13][1] = disk[13][2] = 0xFF;
    assert(fat.mount());
    /* Populate directory entries past byte 255 of the same sector. */
    for (i = 0; i < 12; ++i) {
        name[4] = (char)('A' + i);
        h = fat.open(name, 1);
        assert(h >= 0);
        fat.close(h);
    }
    for (i = 0; i < sizeof(source); ++i) source[i] = (unsigned char)(i * 7U);
    h = fat.open(name, 1);
    assert(h >= 0);
    assert(fat.write(h, source, sizeof(source)) == sizeof(source));
    fat.close(h);
    /* Mount afresh: data and directory size must persist on disk. */
    assert(fat.mount());
    h = fat.open(name, 0);
    assert(h >= 0);
    assert(fat.read(h, result, sizeof(result)) == sizeof(result));
    assert(memcmp(source, result, sizeof(source)) == 0);
    assert(fat.read(h, result, 1) == 0);
    assert(fat.seek(h, -5, 2) == 1395);
    assert(fat.read(h, result, 5) == 5);
    assert(memcmp(source + 1395, result, 5) == 0);
    assert(fat.seek(h, -1401, 2) == -1);
    assert(fat.seek(h, 1401, 0) == -1);
    assert(fat.seek(h, 0, 1) == 1400);
    assert(fat.seek(h, 0, 0) == 0);
    fat.close(h);
    {
        Protected286Word cursor = 0;
        char found[13];
        for (i = 0; i < 12; ++i) {
            assert(fat.nextRootEntry(&cursor, found) == 1);
            name[4] = (char)('A' + i);
            assert(!strcmp(found, name));
        }
        assert(fat.nextRootEntry(&cursor, found) == 0);
        assert(fat.nextRootEntry(&cursor, found) == 0);
    }
    /* FAT16 with a long occupied prefix: allocation must read each FAT
     * sector once, not issue thousands of reads for individual entries. */
    memset(disk, 0, sizeof(disk));
    disk[0][0] = 0xEB;
    put16(11, 512); disk[0][13] = 1; put16(14, 1);
    disk[0][16] = 2; put16(17, 32); put16(19, 8192); put16(22, 32);
    disk[0][510] = 0x55; disk[0][511] = 0xAA;
    for (i = 0; i < 3000; ++i) {
        unsigned sector = 1 + i / 256, at = (i % 256) * 2;
        disk[sector][at] = disk[sector][at + 1] = 0xFF;
        disk[sector + 32][at] = disk[sector + 32][at + 1] = 0xFF;
    }
    assert(fat.mount());
    h = fat.open("FAT16.DAT", 1);
    assert(h >= 0);
    sectorReads = 0;
    assert(fat.write(h, source, sizeof(source)) == sizeof(source));
    fat.close(h);
    assert(sectorReads < 100);
    assert(!memcmp(disk[1], disk[33], 32 * 512));
    assert(fat.mount());
    h = fat.open("FAT16.DAT", 0);
    assert(h >= 0);
    assert(fat.read(h, result, sizeof(result)) == sizeof(result));
    assert(!memcmp(source, result, sizeof(source)));
    fat.close(h);
    return 0;
}
