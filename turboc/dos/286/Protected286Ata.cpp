#include "Protected286Ata.h"
#include "Protected286Ports.h"

#define ATA_DATA 0x1F0
#define ATA_COUNT 0x1F2
#define ATA_LBA0 0x1F3
#define ATA_LBA1 0x1F4
#define ATA_LBA2 0x1F5
#define ATA_DRIVE 0x1F6
#define ATA_STATUS 0x1F7
#define ATA_COMMAND 0x1F7

#define ATA_BUSY 0x80
#define ATA_READY 0x40
#define ATA_DRQ 0x08
#define ATA_ERROR 0x01

int
Protected286Ata::waitNotBusy()
{
    unsigned long i;
    for (i = 0; i < 0x20000UL; i++) {
        if ((Protected286Ports::inByte(ATA_STATUS) & ATA_BUSY) == 0) {
            return 1;
        }
    }
    return 0;
}

int
Protected286Ata::waitDataReady()
{
    unsigned long i;
    Protected286Byte status;
    for (i = 0; i < 0x20000UL; i++) {
        status = Protected286Ports::inByte(ATA_STATUS);
        if ((status & ATA_BUSY) == 0) {
            return (status & (ATA_ERROR | ATA_DRQ)) == ATA_DRQ;
        }
    }
    return 0;
}

void
Protected286Ata::selectLba(Protected286Dword lba, Protected286Byte command)
{
    Protected286Ports::outByte(ATA_DRIVE,
        (Protected286Byte)(0xE0 | ((lba >> 24) & 0x0F)));
    Protected286Ports::outByte(ATA_COUNT, 1);
    Protected286Ports::outByte(ATA_LBA0, (Protected286Byte)lba);
    Protected286Ports::outByte(ATA_LBA1, (Protected286Byte)(lba >> 8));
    Protected286Ports::outByte(ATA_LBA2, (Protected286Byte)(lba >> 16));
    Protected286Ports::outByte(ATA_COMMAND, command);
}

int
Protected286Ata::identify(Protected286Word far* words)
{
    Protected286Word i;
    if (words == 0 || !waitNotBusy()) return 0;
    Protected286Ports::outByte(ATA_DRIVE, 0xE0);
    Protected286Ports::outByte(ATA_COUNT, 0);
    Protected286Ports::outByte(ATA_LBA0, 0);
    Protected286Ports::outByte(ATA_LBA1, 0);
    Protected286Ports::outByte(ATA_LBA2, 0);
    Protected286Ports::outByte(ATA_COMMAND, 0xEC);
    if (Protected286Ports::inByte(ATA_STATUS) == 0 || !waitDataReady()) return 0;
    for (i = 0; i < 256; i++) words[i] = Protected286Ports::inWord(ATA_DATA);
    return 1;
}

int
Protected286Ata::readSector(Protected286Dword lba, Protected286Word far* words)
{
    Protected286Word i;
    if (words == 0 || !waitNotBusy()) return 0;
    selectLba(lba, 0x20);
    if (!waitDataReady()) return 0;
    for (i = 0; i < 256; i++) words[i] = Protected286Ports::inWord(ATA_DATA);
    return 1;
}

int
Protected286Ata::writeSector(Protected286Dword lba, const Protected286Word far* words)
{
    Protected286Word i;
    if (words == 0 || !waitNotBusy()) return 0;
    selectLba(lba, 0x30);
    if (!waitDataReady()) return 0;
    for (i = 0; i < 256; i++) Protected286Ports::outWord(ATA_DATA, words[i]);
    return waitNotBusy();
}
