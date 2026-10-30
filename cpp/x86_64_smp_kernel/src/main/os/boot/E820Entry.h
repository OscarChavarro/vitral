#ifndef __E820_ENTRY__
#define __E820_ENTRY__
#include <stdint.h>

class E820Entry {
public:
    uint64_t base;
    uint64_t length;
    uint32_t type;
    uint32_t acpi;
};

#endif
