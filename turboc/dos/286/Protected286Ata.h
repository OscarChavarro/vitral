#ifndef __PROTECTED_286_ATA__
#define __PROTECTED_286_ATA__

#include "Protected286Types.h"

/* Primary/master ATA PIO device.  The 286 has no DMA requirement here: PIO
 * keeps the driver small and permits transfers to a selector-backed buffer. */
class Protected286Ata {
private:
    static int waitNotBusy();
    static int waitDataReady();
    static void selectLba(Protected286Dword lba, Protected286Byte command);

public:
    static int identify(Protected286Word far* words);
    static int readSector(Protected286Dword lba, Protected286Word far* words);
    static int writeSector(Protected286Dword lba, const Protected286Word far* words);
};

#endif
