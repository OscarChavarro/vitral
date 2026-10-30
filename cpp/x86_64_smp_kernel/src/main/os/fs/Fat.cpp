#include "fs/Fat.h"
#include "drivers/block/BlockDevice.h"
#include "kernel/WallClock.h"
#include "lib/vitralKernel/Calendar.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/DateTime.h"
#include "lib/vitralKernel/String.h"
#include "fs/FileType.h"
#include "fs/Inode.h"
#include "fs/DirEntry.h"

static uint32_t
le16(const uint8_t *p) { return p[0] | (uint32_t)p[1] << 8; }
static uint32_t
le32(const uint8_t *p) { return le16(p) | le16(p + 2) << 16; }
static void
putLe16(uint8_t *p, uint32_t v) {
    p[0] = (uint8_t)v;
    p[1] = (uint8_t)(v >> 8);
}
static void
putLe32(uint8_t *p, uint32_t v) {
    putLe16(p, v & 0xFFFF);
    putLe16(p + 2, v >> 16);
}

static char
lower(char c) { return c >= 'A' && c <= 'Z' ? (char)(c + 32) : c; }
static char
upper(char c) { return c >= 'a' && c <= 'z' ? (char)(c - 32) : c; }
static bool
isLower(char c) { return c >= 'a' && c <= 'z'; }
static bool
isUpper(char c) { return c >= 'A' && c <= 'Z'; }

static bool
namesEqual(const char *a, const char *b) {
    for (; *a && *b; a++, b++) {
        if (lower(*a) != lower(*b)) {
            return false;
        }
    }
    return *a == *b;
}

static bool
validShortChar(char c) {
    if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
        return true;
    }
    for (const char *s = "!#$%&'()-@^_`{|}~"; *s; s++) {
        if (*s == c) {
            return true;
        }
    }
    return false;
}

static uint8_t
shortChecksum(const uint8_t *name) {
    uint8_t sum = 0;
    for (int i = 0; i < 11; i++) {
        sum = (uint8_t)(((sum & 1) << 7) + (sum >> 1) + name[i]);
    }
    return sum;
}

static void
stamp(uint8_t *raw, bool created) {
    DateTime now;
    Calendar::fromUnix(WallClock::seconds(), now);
    if (now.year < 1980) {
        now.year = 1980;
    }
    uint32_t date = ((uint32_t)(now.year - 1980) << 9) | (now.month << 5) | now.day;
    uint32_t time = (now.hour << 11) | (now.minute << 5) | (now.second / 2);
    if (created) {
        raw[13] = (uint8_t)(now.second % 2 * 100);
        putLe16(raw + 14, time);
        putLe16(raw + 16, date);
        putLe16(raw + 18, date);
    }
    putLe16(raw + 22, time);
    putLe16(raw + 24, date);
}

static const uint8_t LFN_CHARS[13] = {1, 3, 5, 7, 9, 14, 16, 18, 20, 22, 24, 28, 30};

Fat::Fat(BlockDevice *blockDevice)
    : device(blockDevice), fatType(0), bytesPerSector(0), sectorsPerCluster(0), clusterBytes(0), reservedSectors(0),
      fatCount(0), fatSectors(0), rootEntries(0), rootDirSector(0), firstDataSector(0), clusterCount(0),
      rootCluster(0), allocHint(2), endMark(0) {}

Fat *
Fat::mount(BlockDevice *blockDevice) {
    if (!blockDevice) {
        return nullptr;
    }
    Fat *fat = new Fat(blockDevice);
    if (!fat->load()) {
        delete fat;
        return nullptr;
    }
    return fat;
}

bool
Fat::load() {
    uint8_t boot[512];
    if (device->read(0, boot, sizeof(boot)) != (int64_t)sizeof(boot)) {
        return false;
    }
    if (boot[510] != 0x55 || boot[511] != 0xAA) {
        return false;
    }

    bytesPerSector = le16(boot + 11);
    sectorsPerCluster = boot[13];
    reservedSectors = le16(boot + 14);
    fatCount = boot[16];
    rootEntries = le16(boot + 17);
    uint32_t total = le16(boot + 19) ? le16(boot + 19) : le32(boot + 32);
    fatSectors = le16(boot + 22) ? le16(boot + 22) : le32(boot + 36);

    if (bytesPerSector < 512 || bytesPerSector > 4096 || (bytesPerSector & (bytesPerSector - 1))) {
        return false;
    }
    if (sectorsPerCluster == 0 || (sectorsPerCluster & (sectorsPerCluster - 1))) {
        return false;
    }
    if (reservedSectors == 0 || fatCount == 0 || fatSectors == 0 || total == 0) {
        return false;
    }
    if ((uint64_t)total * bytesPerSector > device->sizeBytes()) {
        return false;
    }

    uint32_t rootSectors = (rootEntries * ENTRY_SIZE + bytesPerSector - 1) / bytesPerSector;
    rootDirSector = reservedSectors + fatCount * fatSectors;
    firstDataSector = rootDirSector + rootSectors;
    if (total <= firstDataSector) {
        return false;
    }
    clusterCount = (total - firstDataSector) / sectorsPerCluster;
    fatType = clusterCount < 4085 ? 12 : clusterCount < 65525 ? 16 : 32;
    clusterBytes = bytesPerSector * sectorsPerCluster;
    if (fatType == 32) {
        if (rootEntries != 0) {
            return false;
        }
        rootCluster = le32(boot + 44);
        if (rootCluster < 2 || rootCluster >= clusterCount + 2) {
            return false;
        }
    } else if (rootEntries == 0) {
        return false;
    }
    endMark = fatType == 12 ? 0xFFF : fatType == 16 ? 0xFFFF : 0x0FFFFFFF;
    return true;
}

int64_t
Fat::fatGet(uint32_t cluster) {
    if (cluster < 2 || cluster >= clusterCount + 2) {
        return -Err::IO;
    }
    uint64_t base = (uint64_t)reservedSectors * bytesPerSector;
    uint8_t raw[4];
    if (fatType == 12) {
        if (device->read(base + cluster + cluster / 2, raw, 2) != 2) {
            return -Err::IO;
        }
        uint32_t v = le16(raw);
        return (cluster & 1) ? v >> 4 : v & 0xFFF;
    }
    if (fatType == 16) {
        if (device->read(base + (uint64_t)cluster * 2, raw, 2) != 2) {
            return -Err::IO;
        }
        return le16(raw);
    }
    if (device->read(base + (uint64_t)cluster * 4, raw, 4) != 4) {
        return -Err::IO;
    }
    return le32(raw) & 0x0FFFFFFF;
}

int64_t
Fat::fatSet(uint32_t cluster, uint32_t value) {
    if (cluster < 2 || cluster >= clusterCount + 2) {
        return -Err::IO;
    }
    for (uint32_t copy = 0; copy < fatCount; copy++) {
        uint64_t base = ((uint64_t)reservedSectors + (uint64_t)copy * fatSectors) * bytesPerSector;
        uint8_t raw[4];
        if (fatType == 12) {
            uint64_t at = base + cluster + cluster / 2;
            if (device->read(at, raw, 2) != 2) {
                return -Err::IO;
            }
            uint32_t v = le16(raw);
            v = (cluster & 1) ? (v & 0x000F) | (value << 4) : (v & 0xF000) | (value & 0xFFF);
            putLe16(raw, v);
            if (device->write(at, raw, 2) != 2) {
                return -Err::IO;
            }
        } else if (fatType == 16) {
            putLe16(raw, value);
            if (device->write(base + (uint64_t)cluster * 2, raw, 2) != 2) {
                return -Err::IO;
            }
        } else {
            uint64_t at = base + (uint64_t)cluster * 4;
            if (device->read(at, raw, 4) != 4) {
                return -Err::IO;
            }
            putLe32(raw, (le32(raw) & 0xF0000000) | (value & 0x0FFFFFFF));
            if (device->write(at, raw, 4) != 4) {
                return -Err::IO;
            }
        }
    }
    return 0;
}

int64_t
Fat::nextOf(uint32_t cluster) {
    int64_t v = fatGet(cluster);
    if (v < 0) {
        return v;
    }
    if (v >= (int64_t)(endMark - 7)) {
        return 0;
    }
    if (v < 2 || v >= (int64_t)clusterCount + 2) {
        return -Err::IO;
    }
    return v;
}

uint64_t
Fat::clusterOffset(uint32_t cluster) const {
    return ((uint64_t)firstDataSector + (uint64_t)(cluster - 2) * sectorsPerCluster) * bytesPerSector;
}

int64_t
Fat::zeroCluster(uint32_t cluster) {
    uint8_t zeros[512];
    Memory::set(zeros, 0, sizeof(zeros));
    uint64_t at = clusterOffset(cluster);
    for (uint32_t done = 0; done < clusterBytes; done += sizeof(zeros)) {
        if (device->write(at + done, zeros, sizeof(zeros)) != (int64_t)sizeof(zeros)) {
            return -Err::IO;
        }
    }
    return 0;
}

int64_t
Fat::allocCluster(uint32_t previous) {
    for (uint32_t n = 0; n < clusterCount; n++) {
        uint32_t cluster = 2 + (allocHint - 2 + n) % clusterCount;
        int64_t v = fatGet(cluster);
        if (v < 0) {
            return v;
        }
        if (v != 0) {
            continue;
        }
        int64_t r = fatSet(cluster, endMark);
        if (r == 0 && previous) {
            r = fatSet(previous, cluster);
        }
        if (r == 0) {
            r = zeroCluster(cluster);
        }
        if (r < 0) {
            return r;
        }
        allocHint = cluster + 1 < clusterCount + 2 ? cluster + 1 : 2;
        return cluster;
    }
    return -Err::NOSPC;
}

void
Fat::freeChain(uint32_t first) {
    uint32_t cluster = first;
    while (cluster >= 2) {
        int64_t next = nextOf(cluster);
        fatSet(cluster, 0);
        if (cluster < allocHint) {
            allocHint = cluster;
        }
        if (next <= 0) {
            break;
        }
        cluster = (uint32_t)next;
    }
}

int64_t
Fat::clusterAt(Inode &file, uint32_t index, bool allocate) {
    if (file.ino == 0) {
        if (!allocate) {
            return 0;
        }
        int64_t first = allocCluster(0);
        if (first < 0) {
            return first;
        }
        file.ino = (uint64_t)first;
    }
    uint32_t cluster = (uint32_t)file.ino;
    for (uint32_t i = 0; i < index; i++) {
        int64_t next = nextOf(cluster);
        if (next < 0) {
            return next;
        }
        if (next == 0) {
            if (!allocate) {
                return 0;
            }
            next = allocCluster(cluster);
            if (next < 0) {
                return next;
            }
        }
        cluster = (uint32_t)next;
    }
    return cluster;
}

bool
Fat::isRoot(const Inode &dir) const { return fatType == 32 ? dir.ino == rootCluster : dir.ino == 0; }

int64_t
Fat::dirOffset(const Inode &dir, uint32_t index, bool extend) {
    uint64_t byte = (uint64_t)index * ENTRY_SIZE;
    if (fatType != 32 && dir.ino == 0) {
        if (index >= rootEntries) {
            return -Err::NOENT;
        }
        return (int64_t)((uint64_t)rootDirSector * bytesPerSector + byte);
    }
    uint32_t cluster = (uint32_t)dir.ino;
    for (uint64_t i = 0; i < byte / clusterBytes; i++) {
        int64_t next = nextOf(cluster);
        if (next < 0) {
            return next;
        }
        if (next == 0) {
            if (!extend) {
                return -Err::NOENT;
            }
            next = allocCluster(cluster);
            if (next < 0) {
                return next;
            }
        }
        cluster = (uint32_t)next;
    }
    return (int64_t)(clusterOffset(cluster) + byte % clusterBytes);
}

int64_t
Fat::nextEntry(const Inode &dir, uint32_t &index, Entry &entry) {
    uint16_t lfn[20 * 13];
    bool haveLong = false;
    uint8_t longCheck = 0;
    uint32_t longFirst = 0;
    uint32_t expected = 0;

    for (;;) {
        int64_t at = dirOffset(dir, index, false);
        if (at == -Err::NOENT) {
            return 0;
        }
        if (at < 0) {
            return at;
        }
        uint8_t raw[ENTRY_SIZE];
        if (device->read((uint64_t)at, raw, ENTRY_SIZE) != (int64_t)ENTRY_SIZE) {
            return -Err::IO;
        }
        if (raw[0] == 0x00) {
            return 0;
        }
        uint32_t here = index++;
        if (raw[0] == 0xE5) { haveLong = false; continue; }

        uint8_t attr = raw[11];
        if ((attr & 0x3F) == ATTR_LONG_NAME) {
            uint32_t sequence = raw[0] & 0x3F;
            if (sequence < 1 || sequence > 20) { haveLong = false; continue; }
            if (raw[0] & 0x40) {
                haveLong = true;
                longCheck = raw[13];
                longFirst = here;
                for (uint16_t &c : lfn) {
                    c = 0xFFFF;
                }
            } else if (!haveLong || sequence + 1 != expected || raw[13] != longCheck) {
                haveLong = false;
                continue;
            }
            expected = sequence;
            for (int k = 0; k < 13; k++) {
                lfn[(sequence - 1) * 13 + k] = (uint16_t)le16(raw + LFN_CHARS[k]);
            }
            continue;
        }
        if (attr & ATTR_VOLUME) { haveLong = false; continue; }

        Memory::copy(entry.shortName, raw, 11);
        entry.attr = attr;
        entry.cluster = le16(raw + 26) | (fatType == 32 ? le16(raw + 20) << 16 : 0);
        entry.size = le32(raw + 28);
        entry.offset = (uint64_t)at;
        entry.lastIndex = here;
        entry.firstIndex = here;

        unsigned n = 0;
        uint8_t first = raw[0] == 0x05 ? 0xE5 : raw[0];
        for (int i = 0; i < 8 && raw[i] != ' '; i++) {
            char c = (char)(i == 0 ? first : raw[i]);
            entry.shortText[n++] = (raw[12] & 0x08) ? lower(c) : c;
        }
        if (raw[8] != ' ') {
            entry.shortText[n++] = '.';
            for (int i = 8; i < 11 && raw[i] != ' '; i++) {
                entry.shortText[n++] = (raw[12] & 0x10) ? lower((char)raw[i]) : (char)raw[i];
            }
        }
        entry.shortText[n] = 0;

        if (haveLong && expected == 1 && shortChecksum(raw) == longCheck) {
            unsigned length = 0;
            for (unsigned i = 0; i < 20 * 13 && lfn[i] != 0x0000 && lfn[i] != 0xFFFF && length < DirEntry::NAME_MAX; i++) {
                entry.name[length++] = lfn[i] < 0x80 ? (char)lfn[i] : '?';
            }
            entry.name[length] = 0;
            entry.firstIndex = longFirst;
        } else {
            String::copy(entry.name, entry.shortText, sizeof(entry.name));
        }
        return 1;
    }
}

void
Fat::inodeOf(Fat *fs, const Entry &entry, Inode &out) {
    bool directory = entry.attr & ATTR_DIRECTORY;
    out = Inode();
    out.fs = fs;
    out.mode = directory ? FileType::DIRECTORY : FileType::REGULAR;
    out.size = directory ? 0 : entry.size;
    out.ino = entry.cluster;
    out.aux0 = entry.offset;
}

int64_t
Fat::findEntry(const Inode &dir, const char *name, Entry &entry) {
    uint32_t index = 0;
    for (;;) {
        int64_t r = nextEntry(dir, index, entry);
        if (r < 0) {
            return r;
        }
        if (r == 0) {
            return -Err::NOENT;
        }
        if (String::equals(entry.name, ".") || String::equals(entry.name, "..")) {
            continue;
        }
        if (namesEqual(entry.name, name) || namesEqual(entry.shortText, name)) {
            return 0;
        }
    }
}

bool
Fat::root(Inode &out) {
    out = Inode();
    out.fs = this;
    out.mode = FileType::DIRECTORY;
    out.ino = fatType == 32 ? rootCluster : 0;
    return true;
}

int64_t
Fat::lookup(const Inode &dir, const char *name, Inode &out) {
    if (dir.mode != FileType::DIRECTORY) {
        return -Err::NOTDIR;
    }
    Entry entry;
    int64_t r = findEntry(dir, name, entry);
    if (r < 0) {
        return r;
    }
    inodeOf(this, entry, out);
    return 0;
}

int64_t
Fat::readDir(const Inode &dir, uint64_t &cursor, DirEntry &out) {
    if (dir.mode != FileType::DIRECTORY) {
        return -Err::NOTDIR;
    }
    uint32_t index = (uint32_t)cursor;
    Entry entry;
    for (;;) {
        int64_t r = nextEntry(dir, index, entry);
        if (r <= 0) {
            return r;
        }
        if (String::equals(entry.name, ".") || String::equals(entry.name, "..")) {
            continue;
        }
        out.ino = entry.cluster;
        out.mode = (entry.attr & ATTR_DIRECTORY) ? FileType::DIRECTORY : FileType::REGULAR;
        String::copy(out.name, entry.name, sizeof(out.name));
        cursor = index;
        return 1;
    }
}

int64_t
Fat::refresh(Inode &node) {
    if (node.aux0 == 0) {
        return 0;
    }
    uint8_t raw[ENTRY_SIZE];
    if (device->read(node.aux0, raw, ENTRY_SIZE) != (int64_t)ENTRY_SIZE) {
        return -Err::IO;
    }
    if (raw[0] == 0x00 || raw[0] == 0xE5) {
        return -Err::NOENT;
    }
    node.ino = le16(raw + 26) | (fatType == 32 ? le16(raw + 20) << 16 : 0);
    if (node.mode == FileType::REGULAR) {
        node.size = le32(raw + 28);
    }
    return 0;
}

int64_t
Fat::updateEntry(const Inode &node) {
    if (node.aux0 == 0) {
        return 0;
    }
    uint8_t raw[ENTRY_SIZE];
    if (device->read(node.aux0, raw, ENTRY_SIZE) != (int64_t)ENTRY_SIZE) {
        return -Err::IO;
    }
    putLe16(raw + 26, (uint32_t)node.ino & 0xFFFF);
    if (fatType == 32) {
        putLe16(raw + 20, (uint32_t)(node.ino >> 16));
    }
    if (node.mode == FileType::REGULAR) {
        putLe32(raw + 28, (uint32_t)node.size);
    }
    stamp(raw, false);
    return device->write(node.aux0, raw, ENTRY_SIZE) == (int64_t)ENTRY_SIZE ? 0 : -Err::IO;
}

int64_t
Fat::read(Inode &file, uint64_t offset, void *buffer, size_t count) {
    if (file.mode != FileType::REGULAR) {
        return -Err::ISDIR;
    }
    int64_t r = refresh(file);
    if (r < 0) {
        return r;
    }
    if (offset >= file.size || file.ino == 0) {
        return 0;
    }
    if (count > file.size - offset) {
        count = (size_t)(file.size - offset);
    }

    int64_t first = clusterAt(file, (uint32_t)(offset / clusterBytes), false);
    if (first <= 0) {
        return first;
    }
    uint32_t cluster = (uint32_t)first;
    uint8_t *out = (uint8_t *)buffer;
    size_t done = 0;
    while (done < count) {
        uint32_t inside = (uint32_t)((offset + done) % clusterBytes);
        size_t chunk = clusterBytes - inside < count - done ? clusterBytes - inside : count - done;
        if (device->read(clusterOffset(cluster) + inside, out + done, chunk) != (int64_t)chunk) {
            return done ? (int64_t)done : -Err::IO;
        }
        done += chunk;
        if (done < count) {
            int64_t next = nextOf(cluster);
            if (next <= 0) {
                return done ? (int64_t)done : next;
            }
            cluster = (uint32_t)next;
        }
    }
    return (int64_t)done;
}

int64_t
Fat::grow(Inode &file, uint64_t newSize) {
    uint8_t zeros[512];
    Memory::set(zeros, 0, sizeof(zeros));
    while (file.size < newSize) {
        uint64_t left = newSize - file.size;
        int64_t r = write(file, file.size, zeros, left < sizeof(zeros) ? (size_t)left : sizeof(zeros));
        if (r <= 0) {
            return r < 0 ? r : -Err::IO;
        }
    }
    return 0;
}

int64_t
Fat::write(Inode &file, uint64_t offset, const void *buffer, size_t count) {
    if (file.mode != FileType::REGULAR) {
        return -Err::ISDIR;
    }
    int64_t r = refresh(file);
    if (r < 0) {
        return r;
    }
    if (offset + count > 0xFFFFFFFFull) {
        return -Err::FBIG;
    }
    if (offset > file.size) {
        r = grow(file, offset);
        if (r < 0) {
            return r;
        }
    }
    if (count == 0) {
        return 0;
    }

    uint64_t sizeBefore = file.size;
    uint64_t inoBefore = file.ino;
    int64_t first = clusterAt(file, (uint32_t)(offset / clusterBytes), true);
    int64_t error = 0;
    size_t done = 0;
    if (first < 0) {
        error = first;
    } else {
        uint32_t cluster = (uint32_t)first;
        const uint8_t *in = (const uint8_t *)buffer;
        while (done < count) {
            uint32_t inside = (uint32_t)((offset + done) % clusterBytes);
            size_t chunk = clusterBytes - inside < count - done ? clusterBytes - inside : count - done;
            if (device->write(clusterOffset(cluster) + inside, in + done, chunk) != (int64_t)chunk) { error = -Err::IO; break; }
            done += chunk;
            if (done < count) {
                int64_t next = nextOf(cluster);
                if (next < 0) {
                    error = next;
                    break;
                }
                if (next == 0) {
                    next = allocCluster(cluster);
                }
                if (next < 0) {
                    error = next;
                    break;
                }
                cluster = (uint32_t)next;
            }
        }
    }
    if (offset + done > file.size) {
        file.size = offset + done;
    }
    if (file.size != sizeBefore || file.ino != inoBefore) {
        r = updateEntry(file);
        if (r < 0 && !error) {
            error = r;
        }
    }
    return done ? (int64_t)done : error;
}

int64_t
Fat::truncate(Inode &file, uint64_t newSize) {
    if (file.mode != FileType::REGULAR) {
        return -Err::ISDIR;
    }
    int64_t r = refresh(file);
    if (r < 0) {
        return r;
    }
    if (newSize > 0xFFFFFFFFull) {
        return -Err::FBIG;
    }
    if (newSize > file.size) {
        return grow(file, newSize);
    }
    if (newSize == file.size) {
        return 0;
    }

    uint32_t keep = (uint32_t)((newSize + clusterBytes - 1) / clusterBytes);
    if (keep == 0) {
        if (file.ino) {
            freeChain((uint32_t)file.ino);
        }
        file.ino = 0;
    } else {
        int64_t last = clusterAt(file, keep - 1, false);
        if (last > 0) {
            int64_t rest = nextOf((uint32_t)last);
            if (rest > 0) {
                fatSet((uint32_t)last, endMark);
                freeChain((uint32_t)rest);
            }
        }
    }
    file.size = newSize;
    return updateEntry(file);
}

int64_t
Fat::findFreeSlots(const Inode &dir, uint32_t count) {
    uint32_t index = 0;
    uint32_t runStart = 0;
    uint32_t run = 0;
    bool fixedRoot = fatType != 32 && dir.ino == 0;
    for (;;) {
        int64_t at = dirOffset(dir, index, false);
        uint8_t first = 0;
        if (at != -Err::NOENT) {
            if (at < 0) {
                return at;
            }
            if (device->read((uint64_t)at, &first, 1) != 1) {
                return -Err::IO;
            }
        }
        if (first == 0x00) {
            if (run == 0) {
                runStart = index;
            }
            if (fixedRoot && runStart + count > rootEntries) {
                return -Err::NOSPC;
            }
            return runStart;
        }
        if (first == 0xE5) {
            if (run == 0) {
                runStart = index;
            }
            if (++run == count) {
                return runStart;
            }
        } else {
            run = 0;
        }
        index++;
    }
}

bool
Fat::shortNameExists(const Inode &dir, const uint8_t shortName[11]) {
    uint32_t index = 0;
    Entry entry;
    while (nextEntry(dir, index, entry) > 0) {
        if (Memory::compare(entry.shortName, shortName, 11) == 0) {
            return true;
        }
    }
    return false;
}

int64_t
Fat::prepareShortName(const Inode &dir, const char *name, uint8_t shortName[11], uint8_t &ntCase, bool &needLong) {
    size_t length = String::length(name);
    int dot = -1;
    for (size_t i = 0; i < length; i++) {
        if (name[i] == '.') {
            dot = (int)i;
        }
    }
    size_t baseLength = dot < 0 ? length : (size_t)dot;
    size_t extStart = dot < 0 ? length : (size_t)dot + 1;
    size_t extLength = length - extStart;

    bool fits = baseLength >= 1 && baseLength <= 8 && extLength <= 3 && !(dot >= 0 && extLength == 0);
    bool baseLower = false;
    bool baseUpper = false;
    bool extLower = false;
    bool extUpper = false;
    for (size_t i = 0; i < length && fits; i++) {
        if ((int)i == dot) {
            continue;
        }
        if (!validShortChar(name[i])) {
            fits = false;
        }
        bool inExt = dot >= 0 && (int)i > dot;
        if (isLower(name[i])) {
            (inExt ? extLower : baseLower) = true;
        }
        if (isUpper(name[i])) {
            (inExt ? extUpper : baseUpper) = true;
        }
    }
    if ((baseLower && baseUpper) || (extLower && extUpper)) {
        fits = false;
    }

    Memory::set(shortName, ' ', 11);
    if (fits) {
        for (size_t i = 0; i < baseLength; i++) {
            shortName[i] = (uint8_t)upper(name[i]);
        }
        for (size_t i = 0; i < extLength; i++) {
            shortName[8 + i] = (uint8_t)upper(name[extStart + i]);
        }
        ntCase = (uint8_t)((baseLower ? 0x08 : 0) | (extLower ? 0x10 : 0));
        needLong = false;
        return 0;
    }

    char basis[9];
    size_t basisLength = 0;
    for (size_t i = 0; i < baseLength && basisLength < 8; i++) {
        if (validShortChar(name[i])) {
            basis[basisLength++] = upper(name[i]);
        }
    }
    if (basisLength == 0) {
        basis[0] = '_';
        basisLength = 1;
    }
    uint8_t ext[3] = {' ', ' ', ' '};
    size_t extUsed = 0;
    for (size_t i = extStart; i < length && extUsed < 3 && dot >= 0; i++) {
        if (validShortChar(name[i])) {
            ext[extUsed++] = (uint8_t)upper(name[i]);
        }
    }

    for (uint32_t n = 1; n < 1000000; n++) {
        char tail[8];
        unsigned tailLength = 0;
        for (uint32_t v = n; v; v /= 10) {
            tail[tailLength++] = (char)('0' + v % 10);
        }
        size_t keep = basisLength < 8 - 1 - tailLength ? basisLength : 8 - 1 - tailLength;
        Memory::set(shortName, ' ', 11);
        Memory::copy(shortName, basis, keep);
        shortName[keep] = '~';
        for (unsigned i = 0; i < tailLength; i++) {
            shortName[keep + 1 + i] = (uint8_t)tail[tailLength - 1 - i];
        }
        Memory::copy(shortName + 8, ext, 3);
        if (!shortNameExists(dir, shortName)) {
            ntCase = 0;
            needLong = true;
            return 0;
        }
    }
    return -Err::EXIST;
}

int64_t
Fat::create(const Inode &dir, const char *name, uint32_t type, Inode &out) {
    if (dir.mode != FileType::DIRECTORY) {
        return -Err::NOTDIR;
    }
    if (type != FileType::REGULAR && type != FileType::DIRECTORY) {
        return -Err::PERM;
    }
    size_t length = String::length(name);
    if (length == 0 || length > DirEntry::NAME_MAX) {
        return length ? -Err::NAMETOOLONG : -Err::INVAL;
    }
    for (size_t i = 0; i < length; i++) {
        char c = name[i];
        if (c < 0x20 || c >= 0x7F || String::equals(name, ".") || String::equals(name, "..")) {
            return -Err::INVAL;
        }
        for (const char *bad = "\"*/:<>?\\|"; *bad; bad++) {
            if (*bad == c) {
                return -Err::INVAL;
            }
        }
    }
    Entry existing;
    int64_t r = findEntry(dir, name, existing);
    if (r == 0) {
        return -Err::EXIST;
    }
    if (r != -Err::NOENT) {
        return r;
    }

    uint8_t shortName[11];
    uint8_t ntCase;
    bool needLong;
    r = prepareShortName(dir, name, shortName, ntCase, needLong);
    if (r < 0) {
        return r;
    }
    uint32_t pieces = needLong ? (uint32_t)((length + 12) / 13) : 0;
    int64_t slot = findFreeSlots(dir, pieces + 1);
    if (slot < 0) {
        return slot;
    }

    bool directory = type == FileType::DIRECTORY;
    uint32_t cluster = 0;
    if (directory) {
        int64_t c = allocCluster(0);
        if (c < 0) {
            return c;
        }
        cluster = (uint32_t)c;
    }

    uint8_t checksum = shortChecksum(shortName);
    int64_t error = 0;
    for (uint32_t k = 0; k <= pieces && !error; k++) {
        uint8_t raw[ENTRY_SIZE];
        Memory::set(raw, 0, sizeof(raw));
        if (k < pieces) {
            uint32_t sequence = pieces - k;
            raw[0] = (uint8_t)(sequence | (k == 0 ? 0x40 : 0));
            raw[11] = ATTR_LONG_NAME;
            raw[13] = checksum;
            for (int j = 0; j < 13; j++) {
                size_t position = (size_t)(sequence - 1) * 13 + j;
                uint32_t ch = position < length ? (uint8_t)name[position] : position == length ? 0x0000 : 0xFFFF;
                putLe16(raw + LFN_CHARS[j], ch);
            }
        } else {
            Memory::copy(raw, shortName, 11);
            raw[11] = directory ? ATTR_DIRECTORY : 0x20;
            raw[12] = ntCase;
            stamp(raw, true);
            putLe16(raw + 26, cluster & 0xFFFF);
            if (fatType == 32) {
                putLe16(raw + 20, cluster >> 16);
            }
        }
        int64_t at = dirOffset(dir, (uint32_t)slot + k, true);
        if (at < 0) {
            error = at;
        } else if (device->write((uint64_t)at, raw, ENTRY_SIZE) != (int64_t)ENTRY_SIZE) {
            error = -Err::IO;
        }
        if (k == pieces && !error) {
            out = Inode();
            out.fs = this;
            out.mode = type;
            out.ino = cluster;
            out.aux0 = (uint64_t)at;
        }
    }

    if (!error && directory) {
        Inode self;
        self.fs = this;
        self.mode = FileType::DIRECTORY;
        self.ino = cluster;
        for (uint32_t k = 0; k < 2 && !error; k++) {
            uint8_t raw[ENTRY_SIZE];
            Memory::set(raw, 0, sizeof(raw));
            Memory::set(raw, ' ', 11);
            raw[0] = '.';
            if (k == 1) {
                raw[1] = '.';
            }
            raw[11] = ATTR_DIRECTORY;
            stamp(raw, true);
            uint32_t target = k == 0 ? cluster : (isRoot(dir) ? 0 : (uint32_t)dir.ino);
            putLe16(raw + 26, target & 0xFFFF);
            if (fatType == 32) {
                putLe16(raw + 20, target >> 16);
            }
            int64_t at = dirOffset(self, k, false);
            if (at < 0) {
                error = at;
            } else if (device->write((uint64_t)at, raw, ENTRY_SIZE) != (int64_t)ENTRY_SIZE) {
                error = -Err::IO;
            }
        }
    }
    if (error) {
        if (cluster) {
            freeChain(cluster);
        }
        return error;
    }
    return 0;
}

int64_t
Fat::remove(const Inode &dir, const char *name, bool directory) {
    if (dir.mode != FileType::DIRECTORY) {
        return -Err::NOTDIR;
    }
    Entry entry;
    int64_t r = findEntry(dir, name, entry);
    if (r < 0) {
        return r;
    }
    bool isDirectory = entry.attr & ATTR_DIRECTORY;
    if (isDirectory && !directory) {
        return -Err::ISDIR;
    }
    if (!isDirectory && directory) {
        return -Err::NOTDIR;
    }
    if (isDirectory) {
        Inode sub;
        inodeOf(this, entry, sub);
        uint32_t index = 0;
        Entry inside;
        while ((r = nextEntry(sub, index, inside)) > 0) {
            if (!String::equals(inside.name, ".") && !String::equals(inside.name, "..")) {
                return -Err::NOTEMPTY;
            }
        }
        if (r < 0) {
            return r;
        }
    }
    for (uint32_t i = entry.firstIndex; i <= entry.lastIndex; i++) {
        int64_t at = dirOffset(dir, i, false);
        if (at < 0) {
            return at;
        }
        uint8_t mark = 0xE5;
        if (device->write((uint64_t)at, &mark, 1) != 1) {
            return -Err::IO;
        }
    }
    if (entry.cluster >= 2) {
        freeChain(entry.cluster);
    }
    return 0;
}

int64_t
Fat::sync() { return device->flush(); }
