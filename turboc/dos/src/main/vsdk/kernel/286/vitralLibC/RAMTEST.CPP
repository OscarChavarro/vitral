#include "boot/Protected286PhysicalRange.h"
#include <assert.h>
int main()
{
    Protected286Dword base, pool, limit;
    assert(protected286PlanPhysicalRange(15360, 0x90000UL, &base, &pool, &limit));
    assert(base == 0x110000UL && pool == 0x1A0000UL && limit == 0x1000000UL);
    assert(limit - base == 0xEF0000UL); /* No 64 KiB overrun. */
    assert(protected286PlanPhysicalRange(65535, 0x90000UL, &base, &pool, &limit));
    assert(limit == 0x1000000UL);
    assert(!protected286PlanPhysicalRange(0, 16, &base, &pool, &limit));
    assert(!protected286PlanPhysicalRange(64, 16, &base, &pool, &limit));
    assert(!protected286PlanPhysicalRange(128, 16, &base, &pool, &limit));
    assert(protected286PlanPhysicalRange(192, 16, &base, &pool, &limit));
    assert(!protected286PlanPhysicalRange(15360, 0xFFFFFFFFUL, &base, &pool, &limit));
    return 0;
}
