#ifndef __KERNEL_LOG__
#define __KERNEL_LOG__
#include "lib/vitralKernel/SpinLock.h"
#include <stddef.h>

class KernelLog {
public:
    static constexpr size_t SIZE = 16384;
    static constexpr size_t MESSAGE_MAX = 256;

    static void append(const char *text, size_t length);
    static size_t copy(char *buffer, size_t size);
    static void clear();
    static size_t read(uint64_t &position, char *buffer, size_t size);

private:
    static char data[SIZE];
    static size_t head;
    static size_t used;
    static uint64_t total;
    static SpinLock lock;
};

int
kprintf(const char *fmt, ...);

#endif
