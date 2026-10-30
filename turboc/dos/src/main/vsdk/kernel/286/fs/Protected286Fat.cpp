#include "drivers/Protected286Ata.h"
#include "fs/Protected286Fat.h"
#include "boot/Protected286Loader.h"

long Protected286Fat::seek(int handle, long offset, int origin)
{
    Protected286Dword base, position;
    if (handle < 0 || handle >= 4 || !handles[handle].used) return -1;
    if (origin == 0) base = 0;
    else if (origin == 1) base = handles[handle].position;
    else if (origin == 2) base = handles[handle].size;
    else return -1;
    if (offset < 0) {
        Protected286Dword distance = (Protected286Dword)(-(offset + 1)) + 1UL;
        if (distance > base) return -1;
        position = base - distance;
    }
    else {
        position = base + (Protected286Dword)offset;
        if (position < base) return -1;
    }
    /* Sparse writes are not supported by this small FAT backend. */
    if (position > handles[handle].size || position > 0x7FFFFFFFUL) return -1;
    handles[handle].position = position;
    return (long)position;
}

static Protected286Byte fatScratch[512];

int Protected286Fat::nextRootEntry(Protected286Word* cursor, char far* name)
{
    Protected286Word offset;
    unsigned i, n;
    if (!cursor || !name) return -1;
    while (*cursor < rootEntries) {
        if (!readDirectory(*cursor, fatScratch, 0, &offset)) return -1;
        ++*cursor;
        if (!fatScratch[offset]) { *cursor = rootEntries; return 0; }
        if (fatScratch[offset] == 0xE5 || (fatScratch[offset + 11] & 8)) continue;
        n = 0;
        for (i = 0; i < 8 && fatScratch[offset + i] != ' '; ++i)
            name[n++] = (char)fatScratch[offset + i];
        if (fatScratch[offset + 8] != ' ') {
            name[n++] = '.';
            for (i = 8; i < 11 && fatScratch[offset + i] != ' '; ++i)
                name[n++] = (char)fatScratch[offset + i];
        }
        name[n] = 0;
        return 1;
    }
    return 0;
}

static Protected286Word fatLe16(const Protected286Byte far* p)
{
    return (Protected286Word)(p[0] | ((Protected286Word)p[1] << 8));
}

static void fatPut16(Protected286Byte far* p, Protected286Word value)
{
    p[0] = (Protected286Byte)value;
    p[1] = (Protected286Byte)(value >> 8);
}

static Protected286Dword fatLe32(const Protected286Byte far* p)
{
    return (Protected286Dword)fatLe16(p) |
           ((Protected286Dword)fatLe16(p + 2) << 16);
}

static void fatPut32(Protected286Byte far* p, Protected286Dword value)
{
    fatPut16(p, (Protected286Word)value);
    fatPut16(p + 2, (Protected286Word)(value >> 16));
}

Protected286Fat::Protected286Fat()
    : volumeStart(0), sectorsPerCluster(0), reservedSectors(0), fatSectors(0),
      rootEntries(0), rootSectors(0), firstRootSector(0), firstDataSector(0),
      clusterCount(0), nextFreeCluster(2), cachedFatLba(0xFFFFFFFFUL), fat12(0)
{
    Protected286Byte i;
    for (i = 0; i < 4; i++) handles[i].used = 0;
}

int
Protected286Fat::readSector(Protected286Dword lba, Protected286Byte far* bytes)
{
    return Protected286Ata::readSector(lba, (Protected286Word far*)bytes);
}

int
Protected286Fat::writeSector(Protected286Dword lba, const Protected286Byte far* bytes)
{
    return Protected286Ata::writeSector(lba, (const Protected286Word far*)bytes);
}

int
Protected286Fat::mount()
{
    Protected286Dword totalSectors;
    Protected286Dword rootBytes;
    Protected286Dword clusters;
    Protected286Dword partitionStart;
    Protected286Byte partitionType;
    Protected286Byte i;

    if (!readSector(0, fatScratch)) return 0;
    if (fatScratch[510] != 0x55 || fatScratch[511] != 0xAA) return 0;
    /* Accept a superfloppy too; otherwise use primary partition zero. */
    if (fatScratch[0] == 0xEB || fatScratch[0] == 0xE9) {
        volumeStart = 0;
    }
    else {
        partitionType = fatScratch[450];
        partitionStart = fatLe32(fatScratch + 454);
        if (partitionStart == 0 ||
            (partitionType != 0x01 && partitionType != 0x04 &&
             partitionType != 0x06 && partitionType != 0x0E)) return 0;
        volumeStart = partitionStart;
        if (!readSector(volumeStart, fatScratch)) return 0;
    }
    if (fatLe16(fatScratch + 11) != 512) return 0;
    sectorsPerCluster = fatScratch[13];
    reservedSectors = fatLe16(fatScratch + 14);
    rootEntries = fatLe16(fatScratch + 17);
    totalSectors = fatLe16(fatScratch + 19);
    if (totalSectors == 0) totalSectors = fatLe32(fatScratch + 32);
    fatSectors = fatLe16(fatScratch + 22);
    if (sectorsPerCluster == 0 || reservedSectors == 0 || rootEntries == 0 ||
        totalSectors == 0 || fatSectors == 0 || fatScratch[16] == 0) return 0;
    rootBytes = (Protected286Dword)rootEntries * 32UL;
    rootSectors = (Protected286Word)((rootBytes + 511U) / 512U);
    firstRootSector = (Protected286Word)(reservedSectors + fatScratch[16] * fatSectors);
    firstDataSector = (Protected286Word)(firstRootSector + rootSectors);
    if (totalSectors <= firstDataSector) return 0;
    clusters = (totalSectors - firstDataSector) / sectorsPerCluster;
    if (clusters == 0 || clusters >= 65525UL) return 0;
    clusterCount = (Protected286Word)clusters;
    nextFreeCluster = 2;
    cachedFatLba = 0xFFFFFFFFUL;
    fat12 = clusterCount < 4085U;
    for (i = 0; i < 4; i++) handles[i].used = 0;
    return 1;
}

int
Protected286Fat::format83(const char* path, Protected286Byte far* name)
{
    unsigned i, base = 0, ext = 0;
    int dot = 0;
    if (!path || !*path || !name) return 0;
    for (i = 0; i < 11; ++i) name[i] = ' ';
    while (*path) {
        char c = *path++;
        if (c == '.') { if (dot || !base) return 0; dot = 1; continue; }
        if (c <= ' ' || c == '/' || c == '\\' || c == ':' ||
            c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|') return 0;
        if (c >= 'a' && c <= 'z') c = (char)(c - 'a' + 'A');
        if (dot) { if (ext == 3) return 0; name[8 + ext++] = (Protected286Byte)c; }
        else { if (base == 8) return 0; name[base++] = (Protected286Byte)c; }
    }
    return base != 0 && (!dot || ext != 0);
}

int
Protected286Fat::readDirectory(Protected286Word index, Protected286Byte far* bytes,
                               Protected286Dword* lba, Protected286Word* offset)
{
    Protected286Dword sector;
    if (index >= rootEntries) return 0;
    sector = volumeStart + firstRootSector + ((Protected286Dword)index * 32UL) / 512UL;
    if (!readSector(sector, bytes)) return 0;
    if (lba) *lba = sector;
    if (offset) *offset = (Protected286Word)(((Protected286Dword)index * 32UL) & 511UL);
    return 1;
}

/* Return 1 for an entry, 0 at the end and -1 on I/O or corrupt chains. */
int
Protected286Fat::directoryEntry(Protected286Word directory, Protected286Dword index,
                                Protected286Dword* lba, Protected286Word* offset,
                                int grow)
{
    Protected286Word cluster = directory;
    Protected286Dword hops, entriesPerCluster;
    if (!directory) {
        if (index >= rootEntries) return 0;
        return readDirectory((Protected286Word)index, fatScratch, lba, offset) ? 1 : -1;
    }
    entriesPerCluster = (Protected286Dword)sectorsPerCluster * 16UL;
    hops = index / entriesPerCluster;
    if (hops >= clusterCount) return -1;
    while (hops--) {
        long value;
        if (cluster < 2 || (Protected286Dword)cluster >= (Protected286Dword)clusterCount + 2UL) return -1;
        value = fatValue(cluster);
        if (value < 0) return -1;
        if (value >= (fat12 ? 0x0FF8L : 0xFFF8L)) {
            Protected286Word next;
            if (!grow) return 0;
            next = allocateCluster();
            if (!next || !setCluster(cluster, next)) return -1;
            cluster = next;
        } else {
            if (value < 2 || value >= (long)clusterCount + 2L) return -1;
            cluster = (Protected286Word)value;
        }
    }
    if (cluster < 2 || (Protected286Dword)cluster >= (Protected286Dword)clusterCount + 2UL) return -1;
    index %= entriesPerCluster;
    *lba = clusterLba(cluster) + index / 16UL;
    *offset = (Protected286Word)((index % 16UL) * 32UL);
    return readSector(*lba, fatScratch) ? 1 : -1;
}

int
Protected286Fat::findEntry(Protected286Word directory, const Protected286Byte* name,
                           Protected286Byte* entry, Protected286Dword* lba,
                           Protected286Word* offset)
{
    Protected286Dword index;
    for (index = 0; index < (Protected286Dword)clusterCount * sectorsPerCluster * 16UL + rootEntries; ++index) {
        unsigned i;
        int result = directoryEntry(directory, index, lba, offset, 0);
        if (result <= 0) return result;
        if (!fatScratch[*offset]) return 0;
        if (fatScratch[*offset] == 0xE5 || (fatScratch[*offset + 11] & 0x08)) continue;
        for (i = 0; i < 11 && fatScratch[*offset + i] == name[i]; ++i) {}
        if (i == 11) {
            for (i = 0; i < 32; ++i) entry[i] = fatScratch[*offset + i];
            return 1;
        }
    }
    return -1;
}

int
Protected286Fat::walkDirectories(Protected286Word* directory, const char* path)
{
    char component[13];
    Protected286Byte name[11], entry[32];
    Protected286Dword lba;
    Protected286Word offset;
    while (*path) {
        unsigned n = 0;
        while (*path == '/' || *path == '\\') ++path;
        if (!*path) break;
        while (*path && *path != '/' && *path != '\\') {
            if (n == 12) return 0;
            component[n++] = *path++;
        }
        component[n] = 0;
        if (n == 1 && component[0] == '.') continue;
        if (n == 2 && component[0] == '.' && component[1] == '.') {
            if (!*directory) continue;
            for (n = 0; n < 11; ++n) name[n] = ' ';
            name[0] = name[1] = '.';
        } else if (!format83(component, name)) return 0;
        if (findEntry(*directory, name, entry, &lba, &offset) != 1 || !(entry[11] & 0x10)) return 0;
        *directory = fatLe16(entry + 26);
        if (*directory && (*directory < 2 || (Protected286Dword)*directory >= (Protected286Dword)clusterCount + 2UL)) return 0;
    }
    return 1;
}

int
Protected286Fat::resolveParent(const char* path, Protected286Word* directory,
                               Protected286Byte* name)
{
    char parent[68], file[13];
    unsigned n = 0, f = 0;
    const char* p = path;
    int absolute = 0;
    if (!p || !*p) return 0;
    if (p[1] == ':') {
        if (p[0] != 'C' && p[0] != 'c') return 0;
        p += 2;
    }
    if (*p == '/' || *p == '\\') absolute = 1;
    *directory = 0;
    if (!absolute && !walkDirectories(directory, protected286CurrentDirectory + 3)) return 0;
    while (*p) {
        char c = *p++;
        if (c == '/' || c == '\\') {
            if (n + f + 1 >= sizeof(parent)) return 0;
            while (f) { unsigned j; for (j = 0; j < f; ++j) parent[n++] = file[j]; f = 0; }
            parent[n++] = '/';
        } else {
            if (f == 12) return 0;
            file[f++] = c;
        }
    }
    if (!f) return 0;
    file[f] = 0; parent[n] = 0;
    return format83(file, name) && walkDirectories(directory, parent);
}

int
Protected286Fat::allocateHandle()
{
    Protected286Byte i;
    for (i = 0; i < 4; i++) if (!handles[i].used) { handles[i].used = 1; return i; }
    return -1;
}

long
Protected286Fat::fatValue(Protected286Word cluster)
{
    Protected286Dword byte = fat12 ? (Protected286Dword)cluster + cluster / 2U :
                                    (Protected286Dword)cluster * 2UL;
    Protected286Word value;
    Protected286Dword lba = volumeStart + reservedSectors + byte / 512UL;
    Protected286Word at = (Protected286Word)(byte & 511UL);
    if (cachedFatLba != lba) {
        cachedFatLba = 0xFFFFFFFFUL;
        if (!readSector(lba, fatCache)) return -1L;
        cachedFatLba = lba;
    }
    value = fatCache[at];
    if (at == 511) {
        cachedFatLba = 0xFFFFFFFFUL;
        if (!readSector(lba + 1, fatCache)) return -1L;
        cachedFatLba = lba + 1;
        value |= (Protected286Word)fatCache[0] << 8;
    }
    else value |= (Protected286Word)fatCache[at + 1] << 8;
    return fat12 ? ((cluster & 1) ? value >> 4 : value & 0x0FFF) : value;
}

Protected286Word
Protected286Fat::nextCluster(Protected286Word cluster)
{
    long value = fatValue(cluster);
    if (value < 0) return 0;
    if (value >= (fat12 ? 0x0FF8 : 0xFFF8)) return 0;
    return value;
}

int
Protected286Fat::setCluster(Protected286Word cluster, Protected286Word value)
{
    Protected286Dword byte = fat12 ? (Protected286Dword)cluster + cluster / 2U :
                                    (Protected286Dword)cluster * 2UL;
    Protected286Dword lba = volumeStart + reservedSectors + byte / 512UL;
    Protected286Word at = (Protected286Word)(byte & 511UL);
    Protected286Byte copy;
    Protected286Word old;
    cachedFatLba = 0xFFFFFFFFUL;
    if (at == 511) return 0; /* FAT12 entries are not split by supported volumes. */
    for (copy = 0; copy < 2; copy++) {
        if (!readSector(lba + (Protected286Dword)copy * fatSectors, fatScratch)) return 0;
        old = fatLe16(fatScratch + at);
        if (fat12) {
            old = (cluster & 1) ? (old & 0x000F) | (value << 4) :
                                  (old & 0xF000) | (value & 0x0FFF);
        } else old = value;
        fatPut16(fatScratch + at, old);
        if (!writeSector(lba + (Protected286Dword)copy * fatSectors, fatScratch)) return 0;
    }
    return 1;
}

Protected286Word
Protected286Fat::allocateCluster()
{
    Protected286Word cluster;
    Protected286Word sector;
    Protected286Word visited;
    // Walk a FAT sector once, and resume after the last allocation. Scanning
    // from cluster 2 for every image block made export appear to hang.
    for (visited = 0; visited < clusterCount; ++visited) {
        cluster = nextFreeCluster++;
        if (nextFreeCluster >= (Protected286Word)(clusterCount + 2U)) nextFreeCluster = 2;
        long value = fatValue(cluster);
        if (value < 0) return 0;
        if (value == 0) {
            if (!setCluster(cluster, fat12 ? 0x0FFF : 0xFFFF)) return 0;
            for (sector = 0; sector < sectorsPerCluster; sector++) {
                Protected286Word i;
                for (i = 0; i < 512; i++) fatScratch[i] = 0;
                if (!writeSector(clusterLba(cluster) + sector, fatScratch)) return 0;
            }
            return cluster;
        }
    }
    return 0;
}

Protected286Dword
Protected286Fat::clusterLba(Protected286Word cluster)
{
    return volumeStart + firstDataSector +
           (Protected286Dword)(cluster - 2U) * sectorsPerCluster;
}

Protected286Word
Protected286Fat::clusterAt(Handle* handle, Protected286Dword index, Protected286Byte allocate)
{
    Protected286Word cluster = handle->firstCluster;
    Protected286Word next;
    if (cluster == 0 && allocate) {
        cluster = allocateCluster();
        handle->firstCluster = cluster;
    }
    while (cluster != 0 && index-- != 0) {
        next = nextCluster(cluster);
        if (next == 0 && allocate) {
            next = allocateCluster();
            if (next == 0 || !setCluster(cluster, next)) return 0;
        }
        cluster = next;
    }
    return cluster;
}

int
Protected286Fat::updateDirectory(const Handle* handle)
{
    if (!readSector(handle->directoryLba, fatScratch)) return 0;
    fatPut16(fatScratch + handle->directoryOffset + 26, handle->firstCluster);
    fatPut32(fatScratch + handle->directoryOffset + 28, handle->size);
    return writeSector(handle->directoryLba, fatScratch);
}

int
Protected286Fat::open(const char* path, Protected286Byte writable)
{
    Protected286Byte entry[32];
    Protected286Dword lba;
    Protected286Word offset;
    Protected286Dword index;
    Protected286Word directory;
    Protected286Byte wanted[11];
    int found;
    int slot = allocateHandle();
    if (slot < 0) return -1;
    if (!resolveParent(path, &directory, wanted)) { handles[slot].used = 0; return -1; }
    found = findEntry(directory, wanted, entry, &lba, &offset);
    if (found < 0) { handles[slot].used = 0; return -1; }
    if (!found) {
        int placed = 0;
        if (!writable) { handles[slot].used = 0; return -1; }
        for (index = 0; index < (Protected286Dword)clusterCount * sectorsPerCluster * 16UL + rootEntries; ++index) {
            int result = directoryEntry(directory, index, &lba, &offset, 1);
            if (result <= 0) break;
            if (fatScratch[offset] == 0 || fatScratch[offset] == 0xE5) {
                Protected286Byte i;
                for (i = 0; i < 32; i++) fatScratch[offset + i] = 0;
                for (i = 0; i < 11; i++) fatScratch[offset + i] = wanted[i];
                fatScratch[offset + 11] = 0x20;
                if (!writeSector(lba, fatScratch)) break;
                placed = 1;
                break;
            }
        }
        if (!placed) { handles[slot].used = 0; return -1; }
        handles[slot].firstCluster = 0;
        handles[slot].size = 0;
    } else {
        if (entry[11] & 0x10) { handles[slot].used = 0; return -1; }
        handles[slot].firstCluster = fatLe16(entry + 26);
        handles[slot].size = writable ? 0 : fatLe32(entry + 28);
        if (writable) handles[slot].firstCluster = 0;
    }
    handles[slot].position = 0;
    handles[slot].directoryLba = lba;
    handles[slot].directoryOffset = offset;
    handles[slot].writable = writable;
    return slot;
}

long
Protected286Fat::read(int slot, void far* destination, long count)
{
    Handle* handle;
    Protected286Byte far* out = (Protected286Byte far*)destination;
    long done = 0;
    if (slot < 0 || slot >= 4 || !handles[slot].used || count < 0) return -1L;
    handle = &handles[slot];
    while (done < count && handle->position < handle->size) {
        Protected286Dword inCluster = handle->position % ((Protected286Dword)sectorsPerCluster * 512UL);
        Protected286Word cluster = clusterAt(handle, handle->position / ((Protected286Dword)sectorsPerCluster * 512UL), 0);
        Protected286Word at = (Protected286Word)(inCluster & 511UL);
        Protected286Word take = (Protected286Word)(512U - at);
        Protected286Dword remaining = handle->size - handle->position;
        Protected286Word i;
        if (cluster == 0 || !readSector(clusterLba(cluster) + inCluster / 512UL, fatScratch)) break;
        if ((long)take > count - done) take = (Protected286Word)(count - done);
        if ((Protected286Dword)take > remaining) take = (Protected286Word)remaining;
        for (i = 0; i < take; i++) out[done + i] = fatScratch[at + i];
        handle->position += take; done += take;
    }
    return done;
}

long
Protected286Fat::write(int slot, const void far* source, long count)
{
    Handle* handle;
    const Protected286Byte far* in = (const Protected286Byte far*)source;
    long done = 0;
    if (slot < 0 || slot >= 4 || !handles[slot].used || !handles[slot].writable || count < 0) return -1L;
    handle = &handles[slot];
    while (done < count) {
        Protected286Dword inCluster = handle->position % ((Protected286Dword)sectorsPerCluster * 512UL);
        Protected286Word cluster = clusterAt(handle, handle->position / ((Protected286Dword)sectorsPerCluster * 512UL), 1);
        Protected286Word at = (Protected286Word)(inCluster & 511UL);
        Protected286Word take = (Protected286Word)(512U - at);
        Protected286Word i;
        if (cluster == 0 || !readSector(clusterLba(cluster) + inCluster / 512UL, fatScratch)) break;
        if ((long)take > count - done) take = (Protected286Word)(count - done);
        for (i = 0; i < take; i++) fatScratch[at + i] = in[done + i];
        if (!writeSector(clusterLba(cluster) + inCluster / 512UL, fatScratch)) break;
        handle->position += take; done += take;
        if (handle->position > handle->size) handle->size = handle->position;
    }
    return done;
}

void
Protected286Fat::close(int slot)
{
    if (slot < 0 || slot >= 4 || !handles[slot].used) return;
    if (handles[slot].writable) updateDirectory(&handles[slot]);
    handles[slot].used = 0;
}
