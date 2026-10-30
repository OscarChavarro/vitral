#ifndef __BLOCK_DEVICE__
#define __BLOCK_DEVICE__
#include <stdint.h>
#include <stddef.h>

class BlockDevice {
public:
    static constexpr unsigned NAME_LENGTH = 16;
    static constexpr uint32_t MAX_SECTOR_SIZE = 4096;

    virtual ~BlockDevice() {}
    BlockDevice(const BlockDevice &) = delete;
    BlockDevice &operator=(const BlockDevice &) = delete;

    const char *name() const { return nameBuffer; }
    virtual uint32_t sectorSize() const { return 512; }
    virtual uint64_t sectorCount() const { return 0; }
    virtual bool readOnly() const { return false; }
    virtual int64_t readSectors(uint64_t first, uint32_t count, void *buffer);
    virtual int64_t writeSectors(uint64_t first, uint32_t count, const void *buffer);
    virtual int64_t flush() { return 0; }

    uint64_t sizeBytes() const { return sectorCount() * sectorSize(); }
    int64_t read(uint64_t offset, void *buffer, size_t count);
    int64_t write(uint64_t offset, const void *buffer, size_t count);

protected:
    explicit BlockDevice(const char *deviceName);

private:
    char nameBuffer[NAME_LENGTH];
};

#endif
