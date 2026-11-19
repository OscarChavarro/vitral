#ifndef __PL2303__
#define __PL2303__
#include <stddef.h>
#include <stdint.h>
#include "drivers/tty/Tty.h"
#include "drivers/usb/UsbDevice.h"
#include "lib/vitralKernel/SpinLock.h"

// A Prolific PL2303 USB to RS-232 adapter (067b:2303), as the serial line of a machine without COM1 (i.e. a server
// with only USB). attach() sets the chip up as Linux's pl2303 driver does (its classic init sequence for the H and HX
// types), at 115200 baud 8N1 with DTR and RTS on. The line then carries the kernel log, from its start, and the
// terminal /dev/ttyUSB0, where init runs a shell as it does on /dev/ttyS0.
//
// The bulk IN endpoint is polled as an interrupt pipe, every frame, and what it brings waits in a buffer for the
// terminal. The output (the log and what the terminal writes) also waits in a buffer: pump(), called by the USB
// monitor thread every few milliseconds, sends it through the bulk OUT endpoint, since a bulk transfer waits for the
// device and the terminal writes with its lock held.
class Pl2303 : public UsbInterfaceDriver {
public:
    static constexpr uint16_t VENDOR = 0x067B;
    static constexpr uint16_t PRODUCT = 0x2303;

    static Pl2303 *attach(UsbDevice &device, uint8_t interface, uint8_t bulkOutEndpoint, unsigned outPacket,
                          uint8_t bulkInEndpoint, unsigned inPacket);
    // Sends what the kernel log and the terminal have that the adapter did not send yet; from the USB monitor thread.
    static void pump();
    // Waits up to the given time until the adapter sent all the kernel log (none attached, or failed: at once). For
    // code that may stop the machine next. On CPU 0 the USB monitor thread cannot run meanwhile, so it sends the log
    // itself.
    static void flushLog(unsigned milliseconds);
    // An adapter was found: /dev/ttyUSB0 exists.
    static bool ready() { return terminalAdded; }
    // For /dev/ttyUSB0: the bytes received since the last call, and the bytes to send (dropped when the output buffer
    // is full or no adapter is attached).
    static size_t take(uint8_t *buffer, size_t size);
    static void queue(const char *bytes, size_t count);
    ~Pl2303() override;

    const char *kind() const override { return "serial"; }

private:
    static constexpr uint32_t BAUD = 115200;
    static constexpr uint8_t VENDOR_REQUEST = 0x01;
    static constexpr uint8_t REQUEST_SET_LINE_CODING = 0x20;
    static constexpr uint8_t REQUEST_SET_CONTROL_LINE_STATE = 0x22;
    static constexpr uint16_t CONTROL_DTR = 0x01;
    static constexpr uint16_t CONTROL_RTS = 0x02;
    static constexpr unsigned CHUNK = 256;
    static constexpr unsigned QUEUE = 8192;

    static Pl2303 *adapter;
    static bool terminalAdded;
    static SpinLock lock;
    // Set while pump() sends, so that the monitor thread and flushLog() never send at the same time
    static bool pumping;
    // Bytes received, for the terminal
    static uint8_t input[QUEUE];
    static unsigned inputHead;
    static unsigned inputCount;
    // Bytes the terminal wrote, for the adapter
    static char output[QUEUE];
    static unsigned outputHead;
    static unsigned outputCount;

    UsbDevice &device;
    uint8_t interfaceNumber;
    uint8_t endpoint;
    unsigned maxPacket;
    uint8_t toggle;
    uint64_t position;
    // The log position sent so far, a copy of position kept under the lock for logSent()
    uint64_t sentPosition;
    bool failed;
    void *inPipe;

    Pl2303(UsbDevice &adapterDevice, uint8_t interface, uint8_t bulkOutEndpoint, unsigned packet);
    bool initialize();
    bool vendorRead(uint16_t value);
    bool vendorWrite(uint16_t value, uint16_t index);
    bool send(const char *text, unsigned length);
    void sendLog();
    void sendTerminal();
    static void pumpOnce();
    static bool logSent();
    static void received(void *context, const uint8_t *data, unsigned length);
};

// /dev/ttyUSB0: the terminal on the adapter, a "vt220" of 80x24 as /dev/ttyS0.
class Pl2303Tty : public Tty {
public:
    Pl2303Tty() : Tty("", "ttyUSB0", ROWS, COLUMNS) {}
    const char *type() const override { return "vt220"; }

protected:
    size_t fetch(uint8_t *buffer, size_t size) override { return Pl2303::take(buffer, size); }
    void transmit(const char *bytes, size_t count) override { Pl2303::queue(bytes, count); }

private:
    static constexpr uint16_t ROWS = 24;
    static constexpr uint16_t COLUMNS = 80;
};

#endif
