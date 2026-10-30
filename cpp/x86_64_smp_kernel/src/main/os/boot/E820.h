#ifndef __E820__
#define __E820__
#include "boot/BootInfo.h"

class E820 {
public:
    static void collect(BootInfo &info);
};

#endif
