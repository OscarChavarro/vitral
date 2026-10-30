#ifndef __FAT__
#define __FAT__
#include "fs/Filesystem.h"
#include "fs/Inode.h"
#include "fs/DirEntry.h"

class BlockDevice;

class Fat : public Filesystem {
public:
    static Fat *mount(BlockDevice *device);

    const char *type() const override { return "vfat"; }
    bool root(Inode &out) override;
    int64_t lookup(const Inode &dir, const char *name, Inode &out) override;
    int64_t readDir(const Inode &dir, uint64_t &cursor, DirEntry &out) override;
    int64_t read(Inode &file, uint64_t offset, void *buffer, size_t count) override;
    int64_t write(Inode &file, uint64_t offset, const void *buffer, size_t count) override;
    int64_t truncate(Inode &file, uint64_t size) override;
    int64_t create(const Inode &dir, const char *name, uint32_t type, Inode &out) override;
    int64_t remove(const Inode &dir, const char *name, bool directory) override;
    int64_t refresh(Inode &node) override;
    int64_t sync() override;
    uint32_t blockSize() const override { return clusterBytes; }

    unsigned fatBits() const { return fatType; }
    uint32_t clusters() const { return clusterCount; }

private:
    static constexpr uint32_t ENTRY_SIZE = 32;
    static constexpr uint8_t ATTR_DIRECTORY = 0x10;
    static constexpr uint8_t ATTR_VOLUME = 0x08;
    static constexpr uint8_t ATTR_LONG_NAME = 0x0F;

    class Entry {
    public:
        char name[DirEntry::NAME_MAX + 1];
        char shortText[13];
        uint8_t shortName[11];
        uint8_t attr;
        uint32_t cluster;
        uint32_t size;
        uint32_t firstIndex;
        uint32_t lastIndex;
        uint64_t offset;
    };

    explicit Fat(BlockDevice *blockDevice);
    bool load();

    int64_t fatGet(uint32_t cluster);
    int64_t fatSet(uint32_t cluster, uint32_t value);
    int64_t nextOf(uint32_t cluster);
    int64_t allocCluster(uint32_t previous);
    int64_t zeroCluster(uint32_t cluster);
    void freeChain(uint32_t first);
    int64_t clusterAt(Inode &file, uint32_t index, bool allocate);
    uint64_t clusterOffset(uint32_t cluster) const;
    bool isRoot(const Inode &dir) const;

    int64_t dirOffset(const Inode &dir, uint32_t index, bool extend);
    int64_t nextEntry(const Inode &dir, uint32_t &index, Entry &entry);
    int64_t findEntry(const Inode &dir, const char *name, Entry &entry);
    int64_t findFreeSlots(const Inode &dir, uint32_t count);
    int64_t prepareShortName(const Inode &dir, const char *name, uint8_t shortName[11], uint8_t &ntCase, bool &needLong);
    bool shortNameExists(const Inode &dir, const uint8_t shortName[11]);
    int64_t updateEntry(const Inode &node);
    int64_t grow(Inode &file, uint64_t newSize);
    static void inodeOf(Fat *fs, const Entry &entry, Inode &out);

    BlockDevice *device;
    unsigned fatType;
    uint32_t bytesPerSector;
    uint32_t sectorsPerCluster;
    uint32_t clusterBytes;
    uint32_t reservedSectors;
    uint32_t fatCount;
    uint32_t fatSectors;
    uint32_t rootEntries;
    uint32_t rootDirSector;
    uint32_t firstDataSector;
    uint32_t clusterCount;
    uint32_t rootCluster;
    uint32_t allocHint;
    uint32_t endMark;
};

#endif
