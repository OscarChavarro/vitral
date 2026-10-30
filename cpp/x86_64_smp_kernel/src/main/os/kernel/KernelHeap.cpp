#include "kernel/KernelHeap.h"
#include "kernel/Panic.h"
#include "kernel/PhysicalMemory.h"
#include "lib/vitralKernel/LockGuard.h"

char *KernelHeap::start = nullptr;
size_t KernelHeap::total = 0;
size_t KernelHeap::usedTotal = 0;
KernelHeap::Block *KernelHeap::bins[KernelHeap::BINS];
SpinLock KernelHeap::lock;

unsigned
KernelHeap::binOf(size_t payload) {
    unsigned bin = 0;
    for (size_t units = payload / 16; units > 1 && bin < BINS - 1; units >>= 1) {
        bin++;
    }
    return bin;
}

void
KernelHeap::insertFree(Block *b) {
    unsigned bin = binOf(b->size());
    Links *l = links(b);
    l->previous = nullptr;
    l->next = bins[bin];
    if (bins[bin]) {
        links(bins[bin])->previous = b;
    }
    bins[bin] = b;
}

void
KernelHeap::removeFree(Block *b) {
    Links *l = links(b);
    if (l->previous) {
        links(l->previous)->next = l->next;
    } else {
        bins[binOf(b->size())] = l->next;
    }
    if (l->next) {
        links(l->next)->previous = l->previous;
    }
}

void
KernelHeap::init() {
    uint64_t bytes = PhysicalMemory::totalFrames() * PhysicalMemory::FRAME / 4;
    if (bytes < (2ull << 20)) {
        bytes = 2ull << 20;
    }
    if (bytes > (512ull << 20)) {
        bytes = 512ull << 20;
    }
    uint64_t frames = bytes / PhysicalMemory::FRAME;
    uint64_t base = PhysicalMemory::allocContiguous(frames);
    while (!base && frames > 256) {
        frames /= 2;
        base = PhysicalMemory::allocContiguous(frames);
    }
    if (!base) {
        Panic::halt("cannot allocate the kernel heap");
    }
    start = (char *)base;
    total = frames * PhysicalMemory::FRAME;
    for (Block *&bin : bins) {
        bin = nullptr;
    }
    Block *first = (Block *)start;
    first->sizeAndUsed = total - HEADER;
    first->previousSize = 0;
    insertFree(first);
}

KernelHeap::Block *
KernelHeap::carve(Block *b, size_t need) {
    size_t have = b->size();
    if (have >= need + HEADER + MIN_PAYLOAD) {
        Block *rest = (Block *)((char *)b + HEADER + need);
        rest->sizeAndUsed = have - need - HEADER;
        rest->previousSize = need;
        Block *beyond = after(rest);
        if (beyond) {
            beyond->previousSize = rest->size();
        }
        insertFree(rest);
        have = need;
    }
    b->sizeAndUsed = have | 1;
    usedTotal += have + HEADER;
    return b;
}

void *
KernelHeap::tryAllocate(size_t bytes) {
    size_t need = (bytes + 15) & ~(size_t)15;
    if (need < MIN_PAYLOAD) {
        need = MIN_PAYLOAD;
    }
    if (need > total) {
        return nullptr;
    }
    LockGuard guard(lock);
    for (unsigned bin = binOf(need); bin < BINS; bin++) {
        for (Block *b = bins[bin]; b; b = links(b)->next) {
            if (b->size() < need) {
                continue;
            }
            removeFree(b);
            return (char *)carve(b, need) + HEADER;
        }
    }
    return nullptr;
}

void *
KernelHeap::allocate(size_t bytes) {
    void *p = tryAllocate(bytes);
    if (!p) {
        Panic::halt("kernel heap exhausted");
    }
    return p;
}

void *
KernelHeap::tryAllocateAligned(size_t bytes, size_t alignment) {
    if (alignment <= 16) {
        return tryAllocate(bytes);
    }
    size_t need = (bytes + 15) & ~(size_t)15;
    if (need < MIN_PAYLOAD) {
        need = MIN_PAYLOAD;
    }
    size_t worst = need + alignment + HEADER + MIN_PAYLOAD;
    if (worst > total) {
        return nullptr;
    }
    LockGuard guard(lock);
    for (unsigned bin = binOf(worst); bin < BINS; bin++) {
        for (Block *b = bins[bin]; b; b = links(b)->next) {
            uintptr_t payload = (uintptr_t)b + HEADER;
            uintptr_t aligned = (payload + alignment - 1) & ~(uintptr_t)(alignment - 1);
            if (aligned != payload && aligned - payload < HEADER + MIN_PAYLOAD) {
                aligned += alignment;
            }
            size_t lead = aligned - payload;
            if (b->size() < lead + need) {
                continue;
            }
            removeFree(b);
            if (lead == 0) {
                return (char *)carve(b, need) + HEADER;
            }
            size_t whole = b->size();
            b->sizeAndUsed = lead - HEADER;
            Block *mine = (Block *)(aligned - HEADER);
            mine->sizeAndUsed = whole - lead;
            mine->previousSize = b->size();
            Block *beyond = after(mine);
            if (beyond) {
                beyond->previousSize = mine->size();
            }
            insertFree(b);
            return (char *)carve(mine, need) + HEADER;
        }
    }
    return nullptr;
}

void
KernelHeap::release(void *p) {
    if (!p) {
        return;
    }
    LockGuard guard(lock);
    Block *b = (Block *)((char *)p - HEADER);
    usedTotal -= b->size() + HEADER;
    b->sizeAndUsed = b->size();
    Block *next = after(b);
    if (next && !next->used()) {
        removeFree(next);
        b->sizeAndUsed = b->size() + HEADER + next->size();
    }
    Block *prev = before(b);
    if (prev && !prev->used()) {
        removeFree(prev);
        prev->sizeAndUsed = prev->size() + HEADER + b->size();
        b = prev;
    }
    Block *beyond = after(b);
    if (beyond) {
        beyond->previousSize = b->size();
    }
    insertFree(b);
}

size_t
KernelHeap::sizeOf(const void *p) {
    return p ? ((const Block *)((const char *)p - HEADER))->size() : 0;
}

size_t
KernelHeap::usedBytes() {
    LockGuard guard(lock);
    return usedTotal;
}
