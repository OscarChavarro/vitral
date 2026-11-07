#ifndef __SERIAL_DEVICE__
#define __SERIAL_DEVICE__
#include <stdint.h>
#include "drivers/char/CharDevice.h"

// /dev/ttyS0: COM1 as a character device. Reads are nonblocking (-Err::AGAIN when nothing came in) and writes go out
// as the kernel log does, with '\n' sent as CR LF. Raw bytes: line editing and echo are the reader's.
class SerialDevice : public CharDevice {
public:
    static void init();

    int64_t read(void *buffer, size_t count) override;
    int64_t write(const void *buffer, size_t count) override;
    bool readable() override;

private:
    static constexpr size_t CHUNK = 64;

    SerialDevice() : CharDevice("", "ttyS0") {}
};

#endif
