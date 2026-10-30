#include <new>
#include <stdlib.h>
#include "kernel/KernelHeap.h"

namespace std {
const nothrow_t nothrow = {};

bad_alloc::bad_alloc() noexcept {}
bad_alloc::~bad_alloc() noexcept {}
const char *
bad_alloc::what() const noexcept { return "std::bad_alloc"; }
bad_array_new_length::bad_array_new_length() noexcept {}
bad_array_new_length::~bad_array_new_length() noexcept {}
const char *
bad_array_new_length::what() const noexcept { return "std::bad_array_new_length"; }

static new_handler currentHandler = nullptr;
new_handler
set_new_handler(new_handler handler) noexcept {
    new_handler old = currentHandler;
    currentHandler = handler;
    return old;
}
new_handler
get_new_handler() noexcept { return currentHandler; }
}

void *
operator new(size_t size) {
    for (;;) {
        void *p = KernelHeap::tryAllocate(size);
        if (p) {
            return p;
        }
        std::new_handler handler = std::get_new_handler();
        if (!handler) {
            throw std::bad_alloc();
        }
        handler();
    }
}

void *
operator new[](size_t size) { return operator new(size); }
void *
operator new(size_t size, std::align_val_t alignment) {
    void *p = KernelHeap::tryAllocateAligned(size, (size_t)alignment);
    if (!p) {
        throw std::bad_alloc();
    }
    return p;
}
void *
operator new[](size_t size, std::align_val_t alignment) { return operator new(size, alignment); }
void *
operator new(size_t size, const std::nothrow_t &) noexcept { return KernelHeap::tryAllocate(size); }
void *
operator new[](size_t size, const std::nothrow_t &) noexcept { return KernelHeap::tryAllocate(size); }
void
operator delete(void *pointer) noexcept { KernelHeap::release(pointer); }
void
operator delete[](void *pointer) noexcept { KernelHeap::release(pointer); }
void
operator delete(void *pointer, size_t) noexcept { KernelHeap::release(pointer); }
void
operator delete[](void *pointer, size_t) noexcept { KernelHeap::release(pointer); }
void
operator delete(void *pointer, const std::nothrow_t &) noexcept { KernelHeap::release(pointer); }
void
operator delete[](void *pointer, const std::nothrow_t &) noexcept { KernelHeap::release(pointer); }
void
operator delete(void *pointer, std::align_val_t) noexcept { KernelHeap::release(pointer); }
void
operator delete[](void *pointer, std::align_val_t) noexcept { KernelHeap::release(pointer); }
void
operator delete(void *pointer, size_t, std::align_val_t) noexcept { KernelHeap::release(pointer); }
void
operator delete[](void *pointer, size_t, std::align_val_t) noexcept { KernelHeap::release(pointer); }
