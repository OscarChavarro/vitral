#include "kernel/Paging.h"
#include "kernel/Panic.h"
#include "kernel/PhysicalMemory.h"
#include "lib/vitralKernel/Cpu.h"
#include "lib/vitralKernel/InterruptsOff.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Msr.h"
#include "boot/E820Entry.h"

// From entry 7 to entry 0: UC, UC-, WC, WB, UC, UC-, WC, WB. At power-on, entries 1 and 5 are WT.
static constexpr uint64_t PAT_WITH_WRITE_COMBINING = 0x0007010600070106ull;
static constexpr uint64_t CR0_CACHE_DISABLE = 1ull << 30;
static constexpr uint64_t PAGE = 4096;

static void
reloadRoot() {
    asm volatile("movq %0, %%cr3" : : "r"(Paging::root()) : "memory");
}

uint64_t Paging::rootAddress = 0;
uint64_t Paging::mappedEnd = 0;
uint64_t Paging::tables = 0;
unsigned Paging::patCpus = 0;

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
    reloadRoot();
    initCpu();
}

// Intel SDM vol. 3, "Memory Type Changes": the caches stay off and empty while the PAT changes. The result is the
// power-on table, but for entry 1 (the write-through bit alone), which nothing else uses.
void
Paging::initCpu() {
    if (!Cpu::hasPat()) {
        return;
    }
    InterruptsOff guard;
    uint64_t control;
    asm volatile("movq %%cr0, %0" : "=r"(control));
    asm volatile("movq %0, %%cr0; wbinvd" : : "r"(control | CR0_CACHE_DISABLE) : "memory");
    reloadRoot();
    Msr::write(Msr::PAT, PAT_WITH_WRITE_COMBINING);
    asm volatile("wbinvd" : : : "memory");
    reloadRoot();
    asm volatile("movq %0, %%cr0" : : "r"(control) : "memory");
    if (Msr::read(Msr::PAT) == PAT_WITH_WRITE_COMBINING) {
        __atomic_add_fetch(&patCpus, 1, __ATOMIC_RELAXED);
    }
}

// The entries of 2 MB areas the buffer covers change in place; one it covers in part becomes 4 KB pages, so that what
// shares the area with the buffer keeps its memory type.
bool
Paging::mapWriteCombining(uint64_t base, uint64_t bytes) {
    uint64_t end = (base + bytes + PAGE - 1) & ~(PAGE - 1);
    if (bytes == 0 || patCpus == 0 || end > mappedEnd) {
        return false;
    }
    bool done = true;
    uint64_t *pml4 = (uint64_t *)rootAddress;
    for (uint64_t address = base & ~(PAGE - 1); address < end && done;) {
        uint64_t *pdpt = (uint64_t *)(pml4[address >> 39 & 511] & ~0xFFFull);
        uint64_t &directory = pdpt[address >> 30 & 511];
        if (directory & HUGE) {
            done = false;
            break;
        }
        uint64_t &entry = ((uint64_t *)(directory & ~0xFFFull))[address >> 21 & 511];
        uint64_t area = address & ~(MB2 - 1);
        if ((entry & HUGE) && address == area && end >= area + MB2) {
            entry = (entry & ~CACHE_DISABLE) | WRITE_THROUGH;
            address = area + MB2;
            continue;
        }
        if (entry & HUGE) {
            uint64_t *pages = newTable(PhysicalMemory::ANY);
            for (uint64_t p = 0; p < 512; p++) {
                pages[p] = (area + p * PAGE) | PRESENT | WRITABLE | (entry & UNCACHED);
            }
            entry = (uint64_t)pages | PRESENT | WRITABLE;
        }
        uint64_t *pages = (uint64_t *)(entry & ~0xFFFull);
        for (; address < end && address < area + MB2; address += PAGE) {
            uint64_t &page = pages[address >> 12 & 511];
            page = (page & ~CACHE_DISABLE) | WRITE_THROUGH;
        }
    }
    reloadRoot();
    return done;
}
