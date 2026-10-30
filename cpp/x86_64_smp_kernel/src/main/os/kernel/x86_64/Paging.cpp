#include "kernel/Paging.h"
#include "kernel/Panic.h"
#include "kernel/PhysicalMemory.h"
#include "lib/vitralKernel/Cpu.h"
#include "lib/vitralKernel/Memory.h"
#include "boot/E820Entry.h"

uint64_t Paging::rootAddress = 0;
uint64_t Paging::mappedEnd = 0;
uint64_t Paging::tables = 0;

uint64_t *
Paging::newTable(uint64_t below) {
    uint64_t frame = PhysicalMemory::allocContiguous(1, below);
    if (!frame) {
        Panic::halt("out of memory building the page tables");
    }
    Memory::set((void *)frame, 0, PhysicalMemory::FRAME);
    tables++;
    return (uint64_t *)frame;
}

bool
Paging::isMemory(const BootInfo &info, uint64_t start, uint64_t end) {
    for (unsigned i = 0; i < info.e820Count; i++) {
        const E820Entry &e = info.e820[i];
        if ((e.type == 1 || e.type == 3 || e.type == 4) && e.base < end && e.base + e.length > start) {
            return true;
        }
    }
    return false;
}

void
Paging::init(const BootInfo &info) {
    uint64_t top = PhysicalMemory::highestAddress();
    mappedEnd = ((top + GB1 - 1) / GB1) * GB1;
    if (mappedEnd < 4 * GB1) {
        mappedEnd = 4 * GB1;
    }
    bool gigabyte = Cpu::hasGigabytePages();

    uint64_t *pml4 = newTable(1ull << 32);
    rootAddress = (uint64_t)pml4;
    for (uint64_t g = 0; g * GB1 < mappedEnd; g++) {
        uint64_t *pdpt;
        if (!(pml4[g >> 9] & PRESENT)) {
            pdpt = newTable(PhysicalMemory::ANY);
            pml4[g >> 9] = (uint64_t)pdpt | PRESENT | WRITABLE;
        } else {
            pdpt = (uint64_t *)(pml4[g >> 9] & ~0xFFFull);
        }
        uint64_t base = g * GB1;
        if (g >= 4 && gigabyte) {
            pdpt[g & 511] = base | PRESENT | WRITABLE | HUGE;
            continue;
        }
        uint64_t *pd = newTable(PhysicalMemory::ANY);
        pdpt[g & 511] = (uint64_t)pd | PRESENT | WRITABLE;
        for (uint64_t i = 0; i < 512; i++) {
            uint64_t addr = base + i * MB2;
            if (addr == 0) {
                uint64_t *pt = newTable(PhysicalMemory::ANY);
                for (uint64_t p = 0; p < 512; p++) {
                    bool vga = p * 4096 >= 0xA0000 && p * 4096 < 0xC0000;
                    pt[p] = p * 4096 | PRESENT | WRITABLE | (vga ? UNCACHED : 0);
                }
                pd[0] = (uint64_t)pt | PRESENT | WRITABLE;
            } else {
                pd[i] = addr | PRESENT | WRITABLE | HUGE | (isMemory(info, addr, addr + MB2) ? 0 : UNCACHED);
            }
        }
    }
    asm volatile("movq %0, %%cr3" : : "r"(rootAddress) : "memory");
}
