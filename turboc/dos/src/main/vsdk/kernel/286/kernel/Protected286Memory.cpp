#include "kernel/Protected286Memory.h"
#include "boot/Protected286Loader.h"

Protected286Byte Protected286Memory::used[PROTECTED_286_BLOCK_COUNT];

void
Protected286Memory::initialize()
{
    Protected286Word i;
    for (i = 0; i < PROTECTED_286_BLOCK_COUNT; i++) {
        Protected286Dword base = PROTECTED_286_PHYSICAL_START +
            (Protected286Dword)i * PROTECTED_286_SEGMENT_BYTES;
        used[i] = (base >= protected286PoolStart &&
            base + PROTECTED_286_SEGMENT_BYTES <= protected286PoolLimit) ? 0 : 2;
    }
}

Protected286Word
Protected286Memory::allocateSegment()
{
    Protected286Word i;
    for (i = 0; i < PROTECTED_286_BLOCK_COUNT; i++) {
        if (used[i] == 0) {
            used[i] = 1;
            return (Protected286Word)(PROTECTED_286_FIRST_MEMORY_SELECTOR + i * 8);
        }
    }
    return 0;
}

void
Protected286Memory::releaseSegment(Protected286Word selector)
{
    Protected286Word index;
    if (selector < PROTECTED_286_FIRST_MEMORY_SELECTOR ||
         (selector & 7) != 0) {
        return;
    }
    index = (Protected286Word)((selector - PROTECTED_286_FIRST_MEMORY_SELECTOR) / 8);
    if (index < PROTECTED_286_BLOCK_COUNT) {
        if (used[index] == 1) used[index] = 0;
    }
}

Protected286Dword
Protected286Memory::physicalAddress(Protected286Word selector)
{
    Protected286Word index;
    if (selector < PROTECTED_286_FIRST_MEMORY_SELECTOR ||
         (selector & 7) != 0) {
        return 0;
    }
    index = (Protected286Word)((selector - PROTECTED_286_FIRST_MEMORY_SELECTOR) / 8);
    if (index >= PROTECTED_286_BLOCK_COUNT || used[index] != 1) {
        return 0;
    }
    return PROTECTED_286_PHYSICAL_START + (Protected286Dword)index * PROTECTED_286_SEGMENT_BYTES;
}

Protected286Word
Protected286Memory::availableSegments()
{
    Protected286Word i;
    Protected286Word count = 0;
    for (i = 0; i < PROTECTED_286_BLOCK_COUNT; i++) {
        if (used[i] == 0) {
            count++;
        }
    }
    return count;
}
