#ifndef __PROTECTED_286_FAT__
#define __PROTECTED_286_FAT__

#include "vitralLibC/Protected286Crt.h"

/* FAT12/FAT16 backend with 8.3 paths on the mounted C: volume. */
class Protected286Fat : public Protected286FileSystem {
private:
    struct Handle {
        Protected286Word firstCluster;
        Protected286Dword size;
        Protected286Dword position;
        Protected286Dword directoryLba;
        Protected286Word directoryOffset;
        Protected286Byte writable;
        Protected286Byte used;
    };

    Protected286Dword volumeStart;
    Protected286Word sectorsPerCluster;
    Protected286Word reservedSectors;
    Protected286Word fatSectors;
    Protected286Word rootEntries;
    Protected286Word rootSectors;
    Protected286Word firstRootSector;
    Protected286Word firstDataSector;
    Protected286Word clusterCount;
    Protected286Word nextFreeCluster;
    Protected286Dword cachedFatLba;
    Protected286Byte fatCache[512];
    Protected286Byte fat12;
    Handle handles[4];

    int readSector(Protected286Dword lba, Protected286Byte far* bytes);
    int writeSector(Protected286Dword lba, const Protected286Byte far* bytes);
    int readDirectory(Protected286Word index, Protected286Byte far* bytes,
                      Protected286Dword* lba, Protected286Word* offset);
    int directoryEntry(Protected286Word directory, Protected286Dword index,
                       Protected286Dword* lba, Protected286Word* offset,
                       int grow);
    int findEntry(Protected286Word directory, const Protected286Byte* name,
                  Protected286Byte* entry, Protected286Dword* lba,
                  Protected286Word* offset);
    int walkDirectories(Protected286Word* directory, const char* path);
    int resolveParent(const char* path, Protected286Word* directory,
                      Protected286Byte* name);
    int format83(const char* path, Protected286Byte far* name);
    int allocateHandle();
    long fatValue(Protected286Word cluster);
    Protected286Word nextCluster(Protected286Word cluster);
    int setCluster(Protected286Word cluster, Protected286Word value);
    Protected286Word allocateCluster();
    Protected286Dword clusterLba(Protected286Word cluster);
    Protected286Word clusterAt(Handle* handle, Protected286Dword index,
                                Protected286Byte allocate);
    int updateDirectory(const Handle* handle);

public:
    Protected286Fat();
    int mount();
    virtual int open(const char* path, Protected286Byte writable);
    virtual long read(int handle, void far* destination, long count);
    virtual long write(int handle, const void far* source, long count);
    virtual void close(int handle);
    virtual long seek(int handle, long offset, int origin);
    virtual int nextRootEntry(Protected286Word* cursor, char far* name);
};

#endif
