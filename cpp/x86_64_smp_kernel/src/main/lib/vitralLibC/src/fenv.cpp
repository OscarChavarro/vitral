#include <fenv.h>

namespace {
const unsigned short X87_ROUNDING_MASK = 0xc00;
const unsigned int SSE_ROUNDING_SHIFT = 3;
}

extern "C" int
fegetround(void) {
    unsigned short control;
    __asm__ volatile("fnstcw %0" : "=m"(control));
    return control & X87_ROUNDING_MASK;
}

extern "C" int
fesetround(int mode) {
    if ((mode & ~X87_ROUNDING_MASK) != 0) {
        return 1;
    }
    unsigned short control;
    __asm__ volatile("fnstcw %0" : "=m"(control));
    control = (unsigned short)((control & ~X87_ROUNDING_MASK) | mode);
    __asm__ volatile("fldcw %0" : : "m"(control));
#if defined(__SSE__)
    unsigned int status;
    __asm__ volatile("stmxcsr %0" : "=m"(status));
    status = (status & ~((unsigned int)X87_ROUNDING_MASK << SSE_ROUNDING_SHIFT)) |
             ((unsigned int)mode << SSE_ROUNDING_SHIFT);
    __asm__ volatile("ldmxcsr %0" : : "m"(status));
#endif
    return 0;
}
