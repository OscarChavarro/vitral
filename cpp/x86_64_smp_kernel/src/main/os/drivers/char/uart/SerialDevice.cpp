#include "drivers/char/uart/SerialDevice.h"
#include "drivers/char/CharDevices.h"
#include "drivers/char/uart/Uart.h"
#include "kernel/KernelLog.h"

void
SerialDevice::init() {
    if (!Uart::enableReceive()) {
        return;
    }
    if (!CharDevices::add(new SerialDevice())) {
        kprintf("COM1: cannot register /dev/ttyS0\n");
        return;
    }
    kprintf("COM1: /dev/ttyS0 ready for input, IRQ 4\n");
}

int64_t
SerialDevice::read(void *buffer, size_t count) {
    if (count == 0) {
        return 0;
    }
    size_t got = Uart::read((char *)buffer, count);
    return got ? (int64_t)got : -Err::AGAIN;
}

// In short pieces: Uart::write() keeps interrupts off while it sends.
int64_t
SerialDevice::write(const void *buffer, size_t count) {
    const char *text = (const char *)buffer;
    for (size_t done = 0; done < count; done += CHUNK) {
        Uart::write(text + done, count - done < CHUNK ? count - done : CHUNK);
    }
    return (int64_t)count;
}

bool
SerialDevice::readable() { return Uart::readable(); }
