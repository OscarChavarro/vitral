#ifndef __SPIN_LOCK__
#define __SPIN_LOCK__
#include <stdint.h>

class SpinLock {
public:
    constexpr SpinLock() : locked(0) {}

    uint64_t lock() {
        uintptr_t flags;
        asm volatile("pushf; pop %0; cli" : "=r"(flags) : : "memory");
        while (__atomic_exchange_n(&locked, 1, __ATOMIC_ACQUIRE)) {
            while (__atomic_load_n(&locked, __ATOMIC_RELAXED)) {
                asm volatile("pause");
            }
        }
        return flags;
    }
    void unlock(uint64_t flags) {
        __atomic_store_n(&locked, 0, __ATOMIC_RELEASE);
        if (flags & 0x200) {
            asm volatile("sti" : : : "memory");
        }
    }

private:
    volatile uint32_t locked;
};

#endif
