#ifndef __CHAR_DEVICE__
#define __CHAR_DEVICE__
#include <stdint.h>
#include "fs/Err.h"
#include <stddef.h>

class CharDevice {
public:
    static constexpr unsigned NAME_LENGTH = 16;

    virtual ~CharDevice() {}
    CharDevice(const CharDevice &) = delete;
    CharDevice &operator=(const CharDevice &) = delete;

    const char *directory() const { return directoryBuffer[0] ? directoryBuffer : nullptr; }
    const char *name() const { return nameBuffer; }
    virtual int64_t read(void *buffer, size_t count) = 0;
    virtual int64_t write(const void *, size_t) { return -Err::INVAL; }
    virtual bool readable() = 0;

protected:
    CharDevice(const char *deviceDirectory, const char *deviceName);

private:
    char directoryBuffer[NAME_LENGTH];
    char nameBuffer[NAME_LENGTH];
};

#endif
