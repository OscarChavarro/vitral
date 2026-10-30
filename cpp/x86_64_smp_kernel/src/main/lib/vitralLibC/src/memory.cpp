#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "kernel/KernelHeap.h"

void *
malloc(size_t size) {
    void *p = KernelHeap::tryAllocate(size);
    if (!p) {
        errno = ENOMEM;
    }
    return p;
}

void *
calloc(size_t count, size_t size) {
    size_t total;
    if (__builtin_mul_overflow(count, size, &total)) {
        errno = ENOMEM;
        return nullptr;
    }
    void *p = malloc(total);
    if (p) {
        memset(p, 0, total);
    }
    return p;
}

void *
realloc(void *pointer, size_t size) {
    if (!pointer) {
        return malloc(size);
    }
    if (size == 0) {
        free(pointer);
        return nullptr;
    }
    size_t old = KernelHeap::sizeOf(pointer);
    if (size <= old) {
        return pointer;
    }
    void *grown = malloc(size);
    if (!grown) {
        return nullptr;
    }
    memcpy(grown, pointer, old);
    free(pointer);
    return grown;
}

void
free(void *pointer) { KernelHeap::release(pointer); }

void *
aligned_alloc(size_t alignment, size_t size) {
    void *p = KernelHeap::tryAllocateAligned(size, alignment);
    if (!p) {
        errno = ENOMEM;
    }
    return p;
}

int
posix_memalign(void **out, size_t alignment, size_t size) {
    void *p = KernelHeap::tryAllocateAligned(size, alignment);
    if (!p) {
        return ENOMEM;
    }
    *out = p;
    return 0;
}

size_t
malloc_usable_size(void *pointer) { return KernelHeap::sizeOf(pointer); }
