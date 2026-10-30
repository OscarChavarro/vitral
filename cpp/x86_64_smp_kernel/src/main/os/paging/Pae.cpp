#include "paging/Pae.h"

Pae::Table Pae::pml4 __attribute__((section(".bss.paging")));
Pae::Table Pae::pdpt __attribute__((section(".bss.paging")));
Pae::Table Pae::pd[PD_COUNT] __attribute__((section(".bss.paging")));

void
Pae::zero(Table &t) {
    for (auto &e : t.e) {
        e = 0;
    }
}

void
Pae::build() {
    zero(pml4);
    zero(pdpt);
    pml4.e[0] = addr(&pdpt) | PRESENT | WRITABLE;
    for (int j = 0; j < PD_COUNT; j++) {
        zero(pd[j]);
        pdpt.e[j] = addr(&pd[j]) | PRESENT | WRITABLE;
        for (int i = 0; i < 512; i++) {
            uint32_t phys = (uint32_t)(j * 512 + i) << 21;
            pd[j].e[i] = phys | PRESENT | WRITABLE | PAGE_SIZE_2M;
        }
    }
}

void
Pae::activate() {
    uint32_t cr4;
    asm volatile("movl %0, %%cr3" : : "r"(root()) : "memory");
    asm volatile("movl %%cr4, %0" : "=r"(cr4));
    asm volatile("movl %0, %%cr4" : : "r"(cr4 | (1u << 5)) : "memory");
}

uint32_t
Pae::root() { return addr(&pml4); }
