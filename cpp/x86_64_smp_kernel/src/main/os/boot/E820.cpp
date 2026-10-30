#include "boot/E820.h"
#include "boot/E820Entry.h"

void
E820::collect(BootInfo &info) {
    uint32_t next = 0;
    info.e820Count = 0;
    do {
        E820Entry &e = info.e820[info.e820Count];
        e.acpi = 1;
        uint32_t eax = 0xE820;
        uint32_t ebx = next;
        uint32_t ecx = sizeof(E820Entry);
        uint32_t edx = 0x534D4150;
        bool carry;
        asm volatile("int $0x15"
                     : "+a"(eax), "+b"(ebx), "+c"(ecx), "+d"(edx), "=@ccc"(carry)
                     : "D"((uint16_t)(uintptr_t)&e) : "memory");
        if (carry || eax != 0x534D4150) {
            break;
        }
        next = ebx;
        if (ecx > 20 && !(e.acpi & 1)) {
            continue;
        }
        if (e.length != 0) {
            info.e820Count++;
        }
    } while (next != 0 && info.e820Count < BootInfo::MAX_E820);
}
