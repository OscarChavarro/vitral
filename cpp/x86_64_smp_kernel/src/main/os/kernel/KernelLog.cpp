#include "kernel/KernelLog.h"
#include "kernel/Clock.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Printf.h"
#include "lib/vitralKernel/PrintfImpl.h"
#include "lib/vitralKernel/LockGuard.h"
#include "kernel/KlogOut.h"
#include "drivers/char/uart/Uart.h"

char KernelLog::data[KernelLog::SIZE];
size_t KernelLog::head = 0;
size_t KernelLog::used = 0;
uint64_t KernelLog::total = 0;
SpinLock KernelLog::lock;

void
KernelLog::append(const char *text, size_t length) {
    LockGuard guard(lock);
    for (size_t i = 0; i < length; i++) {
        data[head] = text[i];
        head = (head + 1) % SIZE;
    }
    used = used + length < SIZE ? used + length : SIZE;
    total += length;
}

size_t
KernelLog::copy(char *buffer, size_t size) {
    if (size == 0) {
        return 0;
    }
    LockGuard guard(lock);
    size_t start = (head + SIZE - used) % SIZE;
    size_t count = used;
    if (used == SIZE) {
        size_t skip = 0;
        while (skip < count && data[(start + skip) % SIZE] != '\n') {
            skip++;
        }
        if (skip < count) {
            skip++;
        }
        start = (start + skip) % SIZE;
        count -= skip;
    }
    if (count > size - 1) {
        size_t skip = count - (size - 1);
        while (skip < count && data[(start + skip - 1) % SIZE] != '\n') {
            skip++;
        }
        start = (start + skip) % SIZE;
        count -= skip;
    }
    for (size_t i = 0; i < count; i++) {
        buffer[i] = data[(start + i) % SIZE];
    }
    buffer[count] = 0;
    return count;
}

size_t
KernelLog::read(uint64_t &position, char *buffer, size_t size) {
    LockGuard guard(lock);
    uint64_t oldest = total - used;
    if (position < oldest) {
        position = oldest;
    }
    if (position >= total) {
        return 0;
    }
    size_t count = total - position < size ? (size_t)(total - position) : size;
    size_t start = (head + SIZE - (size_t)(total - position)) % SIZE;
    for (size_t i = 0; i < count; i++) {
        buffer[i] = data[(start + i) % SIZE];
    }
    position += count;
    return count;
}

void
KernelLog::clear() {
    LockGuard guard(lock);
    head = used = 0;
}

static SpinLock formatLock;

int
kprintf(const char *fmt, ...) {
    char message[KernelLog::MESSAGE_MAX];
    int count;
    {
        LockGuard guard(formatLock);
        KlogOut::dst = message;
        KlogOut::room = sizeof(message);
        va_list ap;
        va_start(ap, fmt);
        count = PrintfImpl::format<KlogOut>(fmt, ap);
        va_end(ap);
        *KlogOut::dst = 0;
    }
    size_t length = 0;
    while (message[length]) {
        length++;
    }

    uint64_t microseconds = Clock::nanoseconds() / 1000;
    char stamp[24];
    Format::print(stamp, sizeof(stamp), "[%5u.%06u] ", (uint32_t)(microseconds / 1000000), (uint32_t)(microseconds % 1000000));
    size_t stampLength = 0;
    while (stamp[stampLength]) {
        stampLength++;
    }
    KernelLog::append(stamp, stampLength);
    KernelLog::append(message, length);
    if (length == 0 || message[length - 1] != '\n') {
        KernelLog::append("\n", 1);
    }

    printf64("%s", message);
    Uart::write(stamp, stampLength);
    Uart::write(message, length);
    if (length == 0 || message[length - 1] != '\n') {
        Uart::write("\n", 1);
    }
    return count;
}
