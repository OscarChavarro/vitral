#ifndef __PROTECTED_286_PORTS__
#define __PROTECTED_286_PORTS__

#include "kernel/Protected286Types.h"

class Protected286Ports {
public:
    static Protected286Byte inByte(Protected286Word port);
    static void outByte(Protected286Word port, Protected286Byte value);
    static Protected286Word inWord(Protected286Word port);
    static void outWord(Protected286Word port, Protected286Word value);
};

#endif
