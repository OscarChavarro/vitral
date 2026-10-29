#ifndef VITRAL_286_PHYSICAL_RANGE_H
#define VITRAL_286_PHYSICAL_RANGE_H
#include "Protected286Types.h"

/* BIOS 88h reports contiguous KiB above 1 MiB. Keep the HMA unused, cap
 * the physical end (not the size) at 16 MiB and leave a full heap segment. */
inline int protected286PlanPhysicalRange(Protected286Word memoryKB,
    Protected286Dword imageBytes, Protected286Dword* base,
    Protected286Dword* poolStart, Protected286Dword* limit)
{
    Protected286Dword end = 0x00100000UL + (Protected286Dword)memoryKB * 1024UL;
    if (end > PROTECTED_286_PHYSICAL_LIMIT) end = PROTECTED_286_PHYSICAL_LIMIT;
    end &= 0xFFFF0000UL;
    *base = 0x00110000UL;
    *limit = end;
    *poolStart = 0;
    if (end <= *base || !imageBytes || imageBytes > end - *base) return 0;
    *poolStart = (*base + imageBytes + 65535UL) & 0xFFFF0000UL;
    return *poolStart < end;
}
#endif
