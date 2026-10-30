#include "drivers/block/BlockDevice.h"
#include "drivers/block/LoopDevice.h"
#include "fs/Err.h"
#include "kernel/Panic.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "drivers/block/BlockDevices.h"

BlockDevice::BlockDevice(const char *deviceName) { String::copy(nameBuffer, deviceName, NAME_LENGTH); }

int64_t
BlockDevice::readSectors(uint64_t, uint32_t, void *) { return -Err::IO; }
int64_t
BlockDevice::writeSectors(uint64_t, uint32_t, const void *) { return -Err::IO; }

int64_t
BlockDevice::read(uint64_t offset, void *buffer, size_t count) {
    uint32_t sector = sectorSize();
    uint64_t size = sizeBytes();
    if (sector == 0 || sector > MAX_SECTOR_SIZE) {
        return -Err::INVAL;
    }
    if (offset >= size) {
        return 0;
    }
    if (count > size - offset) {
        count = size - offset;
    }

    uint8_t *out = (uint8_t *)buffer;
    uint8_t bounce[MAX_SECTOR_SIZE];
    size_t done = 0;
    while (done < count) {
        uint64_t position = offset + done;
        uint64_t index = position / sector;
        uint32_t inside = (uint32_t)(position % sector);
        size_t left = count - done;
        if (inside == 0 && left >= sector) {
            uint32_t whole = (uint32_t)(left / sector);
            int64_t got = readSectors(index, whole, out + done);
            if (got <= 0) {
                return done ? (int64_t)done : (got < 0 ? got : -Err::IO);
            }
            done += (size_t)got * sector;
        } else {
            int64_t got = readSectors(index, 1, bounce);
            if (got != 1) {
                return done ? (int64_t)done : (got < 0 ? got : -Err::IO);
            }
            size_t chunk = sector - inside < left ? sector - inside : left;
            Memory::copy(out + done, bounce + inside, chunk);
            done += chunk;
        }
    }
    return (int64_t)done;
}

int64_t
BlockDevice::write(uint64_t offset, const void *buffer, size_t count) {
    uint32_t sector = sectorSize();
    uint64_t size = sizeBytes();
    if (sector == 0 || sector > MAX_SECTOR_SIZE) {
        return -Err::INVAL;
    }
    if (readOnly()) {
        return -Err::ROFS;
    }
    if (offset >= size) {
        return count ? -Err::NOSPC : 0;
    }
    if (count > size - offset) {
        count = size - offset;
    }

    const uint8_t *in = (const uint8_t *)buffer;
    uint8_t bounce[MAX_SECTOR_SIZE];
    size_t done = 0;
    while (done < count) {
        uint64_t position = offset + done;
        uint64_t index = position / sector;
        uint32_t inside = (uint32_t)(position % sector);
        size_t left = count - done;
        if (inside == 0 && left >= sector) {
            uint32_t whole = (uint32_t)(left / sector);
            int64_t got = writeSectors(index, whole, in + done);
            if (got <= 0) {
                return done ? (int64_t)done : (got < 0 ? got : -Err::IO);
            }
            done += (size_t)got * sector;
        } else {
            int64_t got = readSectors(index, 1, bounce);
            if (got != 1) {
                return done ? (int64_t)done : (got < 0 ? got : -Err::IO);
            }
            size_t chunk = sector - inside < left ? sector - inside : left;
            Memory::copy(bounce + inside, in + done, chunk);
            got = writeSectors(index, 1, bounce);
            if (got != 1) {
                return done ? (int64_t)done : (got < 0 ? got : -Err::IO);
            }
            done += chunk;
        }
    }
    return (int64_t)done;
}
