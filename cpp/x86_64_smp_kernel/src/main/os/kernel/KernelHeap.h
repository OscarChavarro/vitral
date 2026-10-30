#ifndef __KERNEL_HEAP__
#define __KERNEL_HEAP__
#include <stddef.h>
#include <stdint.h>
#include "lib/vitralKernel/SpinLock.h"

class KernelHeap {
public:
    static void init();
    static void *allocate(size_t bytes);
    static void *tryAllocate(size_t bytes);
    static void *tryAllocateAligned(size_t bytes, size_t alignment);
    static void release(void *p);
    static size_t sizeOf(const void *p);
    static size_t sizeBytes() { return total; }
    static size_t usedBytes();

private:
    // 16 bytes in both builds (padded in i386): payloads stay 16-byte aligned, as FXSAVE and operator new expect.
    class alignas(16) Block {
    public:
        size_t sizeAndUsed;
        size_t previousSize;
        size_t size() const { return sizeAndUsed & ~(size_t)1; }
        bool used() const { return sizeAndUsed & 1; }
    };
    class Links {
    public:
        Block *next;
        Block *previous;
    };
    static constexpr size_t HEADER = sizeof(Block);
    static constexpr size_t MIN_PAYLOAD = sizeof(Links);
    static constexpr unsigned BINS = 48;

    static char *start;
    static size_t total;
    static size_t usedTotal;
    static Block *bins[BINS];
    static SpinLock lock;

    static unsigned binOf(size_t payload);
    static Links *links(Block *b) { return (Links *)((char *)b + HEADER); }
    static Block *after(Block *b) {
        char *n = (char *)b + HEADER + b->size();
        return n < start + total ? (Block *)n : nullptr;
    }
    static Block *before(Block *b) {
        return b->previousSize ? (Block *)((char *)b - HEADER - b->previousSize) : nullptr;
    }
    static void insertFree(Block *b);
    static void removeFree(Block *b);
    static Block *carve(Block *b, size_t need);
};

#endif
