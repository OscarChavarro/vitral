#include "kernel/PhysicalMemory.h"
#include "kernel/Panic.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/LockGuard.h"
#include "boot/E820Entry.h"

uint64_t *PhysicalMemory::bitmap = nullptr;
uint64_t PhysicalMemory::frames = 0;
uint64_t PhysicalMemory::highest = 0;
uint64_t PhysicalMemory::usableFrames = 0;
uint64_t PhysicalMemory::freeCount = 0;
uint64_t PhysicalMemory::hint = 0;
SpinLock PhysicalMemory::lock;

void
PhysicalMemory::markUsed(uint64_t f) {
    if (!used(f)) {
        bitmap[f >> 6] |= 1ull << (f & 63);
        freeCount--;
    }
}

void
PhysicalMemory::markFree(uint64_t f) {
    if (used(f)) {
        bitmap[f >> 6] &= ~(1ull << (f & 63));
        freeCount++;
    }
}

void
PhysicalMemory::reserve(uint64_t start, uint64_t end) {
    uint64_t first = start / FRAME;
    uint64_t last = (end + FRAME - 1) / FRAME;
    for (uint64_t f = first; f < last && f < frames; f++) {
        markUsed(f);
    }
}

void
PhysicalMemory::init(const BootInfo &info) {
    for (unsigned i = 0; i < info.e820Count; i++) {
        if (info.e820[i].type == 1 && info.e820[i].base + info.e820[i].length > highest) {
            highest = info.e820[i].base + info.e820[i].length;
        }
    }
    if (highest > ADDRESSABLE) {
        highest = ADDRESSABLE;
    }
    if (highest == 0) {
        Panic::halt("BIOS memory map has no usable RAM");
    }
    frames = highest / FRAME;
    uint64_t bytes = alignUp(((frames + 63) / 64) * 8);

    uint64_t place = 0;
    for (unsigned i = 0; i < info.e820Count && !place; i++) {
        const E820Entry &e = info.e820[i];
        if (e.type != 1) {
            continue;
        }
        uint64_t start = alignUp(e.base < BootInfo::KERNEL_BASE ? BootInfo::KERNEL_BASE : e.base);
        if (start < alignUp(info.kernelEnd) && start + bytes > info.kernelBase) {
            start = alignUp(info.kernelEnd);
        }
        if (start + bytes <= e.base + e.length && start + bytes <= (1ull << 32)) {
            place = start;
        }
    }
    if (!place) {
        Panic::halt("no room for the physical memory bitmap");
    }
    bitmap = (uint64_t *)place;
    Memory::set(bitmap, 0xFF, bytes);
    freeCount = 0;

    for (unsigned i = 0; i < info.e820Count; i++) {
        const E820Entry &e = info.e820[i];
        if (e.type != 1) {
            continue;
        }
        uint64_t first = alignUp(e.base) / FRAME;
        uint64_t last = (e.base + e.length) / FRAME;
        for (uint64_t f = first; f < last && f < frames; f++) {
            if (used(f)) {
                bitmap[f >> 6] &= ~(1ull << (f & 63));
                freeCount++;
            }
        }
    }
    usableFrames = freeCount;

    reserve(0, 0x100000);
    reserve(info.kernelBase, info.kernelEnd);
    reserve(place, place + bytes);
}

uint64_t
PhysicalMemory::allocContiguous(uint64_t count, uint64_t below) {
    LockGuard guard(lock);
    uint64_t limit = (below / FRAME < frames) ? below / FRAME : frames;
    for (int pass = 0; pass < 2; pass++) {
        uint64_t f = pass == 0 ? hint : 0;
        uint64_t end = pass == 0 ? limit : hint;
        uint64_t run = 0;
        uint64_t runStart = 0;
        for (; f < end; f++) {
            if (used(f)) {
                run = 0;
                continue;
            }
            if (run == 0) {
                runStart = f;
            }
            if (++run == count) {
                for (uint64_t k = runStart; k < runStart + count; k++) {
                    markUsed(k);
                }
                hint = runStart + count;
                return runStart * FRAME;
            }
        }
    }
    return 0;
}

uint64_t
PhysicalMemory::allocFrame() { return allocContiguous(1); }

void
PhysicalMemory::freeFrames(uint64_t address, uint64_t count) {
    LockGuard guard(lock);
    uint64_t first = address / FRAME;
    for (uint64_t f = first; f < first + count && f < frames; f++) {
        markFree(f);
    }
    if (first < hint) {
        hint = first;
    }
}
