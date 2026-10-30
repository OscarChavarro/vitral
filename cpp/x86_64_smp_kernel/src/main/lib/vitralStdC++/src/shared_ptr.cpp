#include <memory>

namespace std {
static char sharedPtrLock;

void
__shared_ptr_lock() noexcept {
    while (__atomic_test_and_set(&sharedPtrLock, __ATOMIC_ACQUIRE)) {
        __builtin_ia32_pause();
    }
}

void
__shared_ptr_unlock() noexcept { __atomic_clear(&sharedPtrLock, __ATOMIC_RELEASE); }
}
