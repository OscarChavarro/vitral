#ifndef __PROTECTED_286_MEMORY__
#define __PROTECTED_286_MEMORY__

#include "Protected286Types.h"

#define PROTECTED_286_BLOCK_COUNT 240

class Protected286Memory {
private:
    static Protected286Byte used[PROTECTED_286_BLOCK_COUNT];

public:
    static void initialize();
    static Protected286Word allocateSegment();
    static void releaseSegment(Protected286Word selector);
    static Protected286Dword physicalAddress(Protected286Word selector);
    static Protected286Word availableSegments();
};

#endif
