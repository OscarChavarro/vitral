#include "lib/vitralStdC++/runtime.h"
#include "lib/vitralLibC/math_dispatch.h"

extern void (*const initArrayStart[])() asm("__init_array_start");
extern void (*const initArrayEnd[])() asm("__init_array_end");

void
Runtime::init() {
    MathDispatch::select();
    for (void (*const *f)() = initArrayStart; f != initArrayEnd; f++) {
        (*f)();
    }
}
