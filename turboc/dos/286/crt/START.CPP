#include "CRT.H"
#ifdef __TURBOC__
#include <dos.h>
#endif

static Crt286Callback callbacks[64];
static unsigned callbackCount;
extern "C" void crt286ResetCallbacks() { callbackCount = 0; }

#ifdef __TURBOC__
static unsigned char tableByte(unsigned long physical)
{
    return *(unsigned char far*)MK_FP((unsigned)(0x800 + ((physical - 0x100000UL) >> 16) * 8),
                                      (unsigned)physical);
}
extern "C" int crt286RunInitTable(unsigned long physical, unsigned bytes, int reverse)
{
    if (bytes % 6 || physical < 0x100000UL || physical + bytes > 0x1000000UL) return 0;
    for (unsigned check = 0; check < bytes; check += 6)
        if (tableByte(physical + check) != 1) return 0; /* Far callbacks only. */
    for (int priority = reverse ? 255 : 0; priority >= 0 && priority <= 255;
         priority += reverse ? -1 : 1) {
        for (unsigned i = 0; i < bytes; i += 6) {
            unsigned offset = reverse ? bytes - 6 - i : i;
            unsigned long entry = physical + offset;
            if (tableByte(entry + 1) != priority) continue;
            unsigned address = tableByte(entry + 2) | ((unsigned)tableByte(entry + 3) << 8);
            unsigned selector = tableByte(entry + 4) | ((unsigned)tableByte(entry + 5) << 8);
            if (!selector || (selector & 7)) return 0;
            Crt286Callback callback = (Crt286Callback)MK_FP(selector, address);
            (*callback)();
        }
    }
    return 1;
}
#endif

extern "C" int crt286AtExit(Crt286Callback function)
{
    if (!function || callbackCount == 64) return -1;
    callbacks[callbackCount++] = function;
    return 0;
}

extern "C" void crt286Finalize()
{
    while (callbackCount) {
        Crt286Callback function = callbacks[--callbackCount];
        (*function)();
    }
}

extern "C" void crt286Construct(const Crt286Callback* first, unsigned count)
{
    unsigned i;
    for (i = 0; i < count; ++i) if (first[i]) (*first[i])();
}
