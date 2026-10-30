#include "kernel/Smp.h"
#include "acpi/Acpi.h"
#include "kernel/Clock.h"
#include "kernel/Fpu.h"
#include "kernel/Gdt.h"
#include "kernel/Interrupts.h"
#include "kernel/KernelLog.h"
#include "kernel/LocalApic.h"
#include "kernel/Paging.h"
#include "kernel/Scheduler.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Cpu.h"
#include "kernel/KernelLog.h"

Smp::LogicalCpu Smp::table[Smp::MAX_CPUS];
unsigned Smp::count = 0;
unsigned Smp::cores = 1;
bool Smp::topologyKnown = false;
volatile bool Smp::apReady = false;
volatile bool Smp::released = false;
uint8_t Smp::indexByApicId[256];

void
Smp::detect() {
    uint32_t ids[MAX_CPUS];
    unsigned found = 0;
    uint32_t bspId = Cpu::apicId();

    if (!Cpu::hasApic() || !Acpi::processorApicIds(ids, MAX_CPUS, found)) {
        found = 0;
    }
    if (found > MAX_CPUS) {
        found = MAX_CPUS;
    }

    count = 0;
    table[count++] = {bspId, 0, 0, 0, State::Online, true, nullptr};
    for (unsigned i = 0; i < found; i++) {
        if (ids[i] != bspId) {
            table[count++] = {ids[i], 0, 0, 0, State::Detected, false, nullptr};
        }
    }

    unsigned smtBits = 0;
    unsigned coreBits = 0;
    topologyKnown = Cpu::topology(smtBits, coreBits);
    for (unsigned i = 0; i < count; i++) {
        LogicalCpu &c = table[i];
        if (topologyKnown) {
            c.thread = c.apicId & ((1u << smtBits) - 1);
            c.core = (c.apicId >> smtBits) & ((1u << coreBits) - 1);
            c.package = c.apicId >> (smtBits + coreBits);
        } else {
            c.package = 0;
            c.core = i;
            c.thread = 0;
        }
    }

    for (auto &i : indexByApicId) {
        i = 0;
    }
    for (unsigned i = 0; i < count; i++) {
        if (table[i].apicId < 256) {
            indexByApicId[table[i].apicId] = (uint8_t)i;
        }
    }

    cores = 0;
    for (unsigned i = 0; i < count; i++) {
        bool seen = false;
        for (unsigned j = 0; j < i; j++) {
            seen |= table[j].package == table[i].package && table[j].core == table[i].core;
        }
        if (!seen) {
            cores++;
        }
    }
}

void
Smp::report() {
    if (!isSmp()) {
        kprintf("SMP not supported, using a single core CPU\n");
        return;
    }
    kprintf("SMP using %u cores\n", count);
    kprintf("  %u logical CPUs = %u physical cores x %u threads%s\n", count, cores,
             threadsPerCore(), topologyKnown ? "" : " (topology unknown: CPUID leaf 0Bh missing)");
    for (unsigned i = 0; i < count && i < 16; i++) {
        const LogicalCpu &c = table[i];
        kprintf("  CPU %u: APIC id %u, package %u core %u thread %u%s\n", i, c.apicId, c.package,
                 c.core, c.thread, c.bsp ? " (BSP, online)" : "");
    }
}

unsigned
Smp::currentIndex() {
    if (!LocalApic::isEnabled()) {
        return 0;
    }
    uint32_t id = LocalApic::id();
    return id < 256 ? indexByApicId[id] : 0;
}

unsigned
Smp::onlineCount() {
    unsigned n = 0;
    for (unsigned i = 0; i < count; i++) {
        n += table[i].state == State::Online;
    }
    return n;
}

void
Smp::wait(uint64_t ticks) {
    uint64_t until = Clock::ticks() + ticks;
    while (Clock::ticks() < until) {
        asm volatile("pause");
    }
}

extern const char trampolineStart[] asm("ap_trampoline_start");
extern const char trampolineEnd[] asm("ap_trampoline_end");
extern char trampolineCr3[] asm("ap_data_cr3");
extern char trampolineStack[] asm("ap_data_stack");
extern char trampolineEntry[] asm("ap_data_entry");

bool
Smp::startProcessor(unsigned index) {
    char *stack = new char[AP_STACK_SIZE];
    auto slot = [](char *symbol) { return (uint64_t *)(TRAMPOLINE + (symbol - (char *)trampolineStart)); };
    *slot(trampolineCr3) = Paging::root();
    *slot(trampolineStack) = (uint64_t)(uintptr_t)(stack + AP_STACK_SIZE);
    *slot(trampolineEntry) = (uint64_t)(uintptr_t)&Smp::apMain;

    apReady = false;
    uint32_t id = table[index].apicId;
    LocalApic::sendInit(id);
    wait(2);
    for (int attempt = 0; attempt < 2 && !apReady; attempt++) {
        LocalApic::sendStartup(id, TRAMPOLINE >> 12);
        uint64_t until = Clock::ticks() + 2;
        while (!apReady && Clock::ticks() < until) {
            asm volatile("pause");
        }
    }
    return apReady;
}

void
Smp::startProcessors() {
    if (!isSmp() || !LocalApic::supported()) {
        return;
    }
    LocalApic::enable();
    LocalApic::calibrate();
    Memory::copy((void *)TRAMPOLINE, trampolineStart, trampolineEnd - trampolineStart);
    for (unsigned i = 0; i < count; i++) {
        if (table[i].bsp) {
            continue;
        }
        if (!startProcessor(i)) {
            kprintf("  CPU %u (APIC id %u) did not start\n", i, table[i].apicId);
        }
    }
    kprintf("CPUs online: %u of %u\n", onlineCount(), count);
}

void
Smp::apMain() {
    Gdt::load();
    Interrupts::load();
    Fpu::enable();
    LocalApic::enable();
    table[currentIndex()].state = State::Online;
    apReady = true;
    while (!released) {
        asm volatile("pause");
    }
    Scheduler::startCpu();
}
