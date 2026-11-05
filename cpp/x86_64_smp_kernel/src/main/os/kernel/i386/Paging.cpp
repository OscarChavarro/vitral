#include "kernel/Paging.h"

uint64_t Paging::rootAddress = 0;
uint64_t Paging::mappedEnd = 0;
uint64_t Paging::tables = 0;
unsigned Paging::patCpus = 0;

void
Paging::init(const BootInfo &) {
    mappedEnd = 4 * GB1;
}

void
Paging::initCpu() {}

// With paging off there are no page attributes to select a memory type with.
bool
Paging::mapWriteCombining(uint64_t, uint64_t) {
    return false;
}
