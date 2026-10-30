#ifndef __LOCK_GUARD__
#define __LOCK_GUARD__
#include <stdint.h>
#include "lib/vitralKernel/SpinLock.h"

class LockGuard {
public:
    explicit LockGuard(SpinLock &l) : lockRef(l), flags(l.lock()) {}
    ~LockGuard() { lockRef.unlock(flags); }
    LockGuard(const LockGuard &) = delete;
    LockGuard &operator=(const LockGuard &) = delete;

private:
    SpinLock &lockRef;
    uint64_t flags;
};

#endif
