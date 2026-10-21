#ifndef __ATOMIC_LONG__
#define __ATOMIC_LONG__

#include "java/util/concurrent/locks/ReentrantLock.h"


/**
Emulation of `java.util.concurrent.atomic.AtomicLong`. C++98 has no atomic
operations, so the value is protected by a lock.
*/
class AtomicLong {
private:
    mutable ReentrantLock lock;
    long long value;

    AtomicLong(const AtomicLong &other);
    AtomicLong &operator=(const AtomicLong &other);

public:
    AtomicLong() : lock(), value(0) {}
    explicit AtomicLong(long long initialValue) : lock(), value(initialValue) {}

    long long get() const
    {
        ReentrantLockGuard guard(lock);
        return value;
    }

    void set(long long newValue)
    {
        ReentrantLockGuard guard(lock);
        value = newValue;
    }

    long long addAndGet(long long delta)
    {
        ReentrantLockGuard guard(lock);
        value += delta;
        return value;
    }

    long long incrementAndGet()
    {
        ReentrantLockGuard guard(lock);
        value++;
        return value;
    }
};


#endif
