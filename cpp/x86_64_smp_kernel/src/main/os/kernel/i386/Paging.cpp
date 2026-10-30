#include "kernel/Paging.h"

uint64_t Paging::rootAddress = 0;
uint64_t Paging::mappedEnd = 0;
uint64_t Paging::tables = 0;

void
Paging::init(const BootInfo &) {
    mappedEnd = 4 * GB1;
}
