#include "drivers/usb/pl2303/Pl2303.h"
#include "drivers/char/CharDevices.h"
#include "kernel/Clock.h"
#include "kernel/KernelLog.h"
#include "kernel/Scheduler.h"
#include "lib/vitralKernel/LockGuard.h"

Pl2303 *Pl2303::adapter = nullptr;
bool Pl2303::terminalAdded = false;
SpinLock Pl2303::lock;
bool Pl2303::pumping = false;
uint8_t Pl2303::input[Pl2303::QUEUE];
unsigned Pl2303::inputHead = 0;
unsigned Pl2303::inputCount = 0;
char Pl2303::output[Pl2303::QUEUE];
unsigned Pl2303::outputHead = 0;
unsigned Pl2303::outputCount = 0;

Pl2303::Pl2303(UsbDevice &adapterDevice, uint8_t interface, uint8_t bulkOutEndpoint, unsigned packet)
    : device(adapterDevice), interfaceNumber(interface), endpoint(bulkOutEndpoint), maxPacket(packet), toggle(0),
      position(0), sentPosition(0), failed(false), inPipe(nullptr) {}

Pl2303 *
Pl2303::attach(UsbDevice &device, uint8_t interface, uint8_t bulkOutEndpoint, unsigned outPacket,
               uint8_t bulkInEndpoint, unsigned inPacket) {
    {
        LockGuard guard(lock);
        if (adapter) {
            kprintf("USB: PL2303 \"%s\" ignored, only one adapter is supported\n", device.name);
            return nullptr;
        }
    }
    Pl2303 *created = new Pl2303(device, interface, bulkOutEndpoint, outPacket);
    if (!created->initialize()) {
        kprintf("USB: PL2303 \"%s\" does not answer its set up requests\n", device.name);
        delete created;
        return nullptr;
    }
    if (bulkInEndpoint) {
        created->inPipe = device.controller->openInterruptPipe(device.address, device.speed, bulkInEndpoint,
                                                               inPacket, received, created);
    }
    {
        LockGuard guard(lock);
        adapter = created;
        inputHead = inputCount = 0;
    }
    if (!terminalAdded) {
        terminalAdded = CharDevices::add(new Pl2303Tty());
    }
    kprintf("USB: PL2303 serial adapter at address %u: kernel log at %u baud, 8N1%s\n", (uint32_t)device.address,
            BAUD, created->inPipe && terminalAdded ? ", terminal /dev/ttyUSB0" : " (output only)");
    return created;
}

Pl2303::~Pl2303() {
    if (inPipe) {
        device.controller->closeInterruptPipe(inPipe);
    }
    LockGuard guard(lock);
    if (adapter == this) {
        adapter = nullptr;
    }
}

bool
Pl2303::vendorRead(uint16_t value) {
    uint8_t answer = 0;
    return device.control(0xC0, VENDOR_REQUEST, value, 0, 1, &answer) >= 0;
}

bool
Pl2303::vendorWrite(uint16_t value, uint16_t index) {
    return device.control(0x40, VENDOR_REQUEST, value, index, 0, nullptr) >= 0;
}

bool
Pl2303::initialize() {
    // The H type has a 8 byte endpoint 0 or the class 2; the HX ones (HXA, HXD, TA, TB) have 64 bytes.
    bool hx = device.deviceClass != 0x02 && device.maxPacket0 == 64;
    bool ok = vendorRead(0x8484) && vendorWrite(0x0404, 0) && vendorRead(0x8484) && vendorRead(0x8383) &&
              vendorRead(0x8484) && vendorWrite(0x0404, 1) && vendorRead(0x8484) && vendorRead(0x8383) &&
              vendorWrite(0, 1) && vendorWrite(1, 0) && vendorWrite(2, hx ? 0x44 : 0x24);
    if (!ok) {
        return false;
    }
    if (hx) {
        // Resets the upstream and downstream data pipes
        vendorWrite(8, 0);
        vendorWrite(9, 0);
    }
    uint8_t lineCoding[7] = {
        (uint8_t)BAUD, (uint8_t)(BAUD >> 8), (uint8_t)(BAUD >> 16), (uint8_t)(BAUD >> 24),
        0,  // one stop bit
        0,  // no parity
        8,  // data bits
    };
    return device.control(0x21, REQUEST_SET_LINE_CODING, 0, interfaceNumber, sizeof(lineCoding), lineCoding) >= 0 &&
           device.control(0x21, REQUEST_SET_CONTROL_LINE_STATE, CONTROL_DTR | CONTROL_RTS, interfaceNumber, 0,
                          nullptr) >= 0;
}

// From the host controller's poll(), with its lock held: only stores the bytes.
void
Pl2303::received(void *, const uint8_t *data, unsigned length) {
    LockGuard guard(lock);
    for (unsigned i = 0; i < length && inputCount < QUEUE; i++) {
        input[(inputHead + inputCount) % QUEUE] = data[i];
        inputCount++;
    }
}

size_t
Pl2303::take(uint8_t *buffer, size_t size) {
    LockGuard guard(lock);
    size_t count = 0;
    while (count < size && inputCount > 0) {
        buffer[count++] = input[inputHead];
        inputHead = (inputHead + 1) % QUEUE;
        inputCount--;
    }
    return count;
}

void
Pl2303::queue(const char *bytes, size_t count) {
    LockGuard guard(lock);
    if (!adapter) {
        return;
    }
    for (size_t i = 0; i < count && outputCount < QUEUE; i++) {
        output[(outputHead + outputCount) % QUEUE] = bytes[i];
        outputCount++;
    }
}

bool
Pl2303::send(const char *text, unsigned length) {
    unsigned offset = 0;
    while (offset < length) {
        int sent = device.controller->bulkOut(device.address, device.speed, endpoint, maxPacket, toggle,
                                              text + offset, length - offset);
        if (sent <= 0) {
            return false;
        }
        offset += (unsigned)sent;
    }
    return true;
}

// The log has bare newlines; a terminal needs carriage returns too.
void
Pl2303::sendLog() {
    char chunk[CHUNK];
    char line[CHUNK * 2];
    for (;;) {
        size_t count = KernelLog::read(position, chunk, sizeof(chunk));
        if (count == 0) {
            return;
        }
        unsigned length = 0;
        for (size_t i = 0; i < count; i++) {
            if (chunk[i] == '\n') {
                line[length++] = '\r';
            }
            line[length++] = chunk[i];
        }
        if (!send(line, length)) {
            failed = true;
            return;
        }
        LockGuard guard(lock);
        sentPosition = position;
    }
}

void
Pl2303::sendTerminal() {
    char chunk[CHUNK];
    for (;;) {
        unsigned count = 0;
        {
            LockGuard guard(lock);
            while (count < CHUNK && outputCount > 0) {
                chunk[count++] = output[outputHead];
                outputHead = (outputHead + 1) % QUEUE;
                outputCount--;
            }
        }
        if (count == 0) {
            return;
        }
        if (!send(chunk, count)) {
            failed = true;
            return;
        }
    }
}

// The transfers wait for the device, so they run without the lock: the adapter is only added and removed by the USB
// core, in the same thread as pump(), and pumping keeps flushLog() out.
void
Pl2303::pump() {
    if (__atomic_exchange_n(&pumping, true, __ATOMIC_ACQUIRE)) {
        return;
    }
    pumpOnce();
    __atomic_store_n(&pumping, false, __ATOMIC_RELEASE);
}

void
Pl2303::pumpOnce() {
    Pl2303 *current;
    {
        LockGuard guard(lock);
        current = adapter;
    }
    if (!current || current->failed) {
        return;
    }
    current->sendLog();
    if (!current->failed) {
        current->sendTerminal();
    }
    if (current->failed) {
        klogf("USB: PL2303 at address %u stopped: a bulk transfer failed\n", (uint32_t)current->device.address);
    }
}

bool
Pl2303::logSent() {
    uint64_t written = KernelLog::written();
    LockGuard guard(lock);
    return !adapter || adapter->failed || adapter->sentPosition >= written;
}

void
Pl2303::flushLog(unsigned milliseconds) {
    uint64_t end = Clock::uptimeMs() + milliseconds;
    while (!logSent() && Clock::uptimeMs() < end) {
        if (Scheduler::currentCpu() == 0) {
            pump();
        }
        asm volatile("pause");
    }
}
