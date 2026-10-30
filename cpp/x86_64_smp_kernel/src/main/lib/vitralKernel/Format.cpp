#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/PrintfImpl.h"
#include "lib/vitralKernel/SpinLock.h"
#include "lib/vitralKernel/LockGuard.h"
#include "lib/vitralKernel/BufferOut.h"

static SpinLock formatLock;

int
Format::print(char *buffer, size_t capacity, const char *fmt, ...) {
    if (capacity == 0) {
        return 0;
    }
    LockGuard guard(formatLock);
    BufferOut::dst = buffer;
    BufferOut::room = capacity;
    va_list ap;
    va_start(ap, fmt);
    int n = PrintfImpl::format<BufferOut>(fmt, ap);
    va_end(ap);
    *BufferOut::dst = 0;
    return n;
}
