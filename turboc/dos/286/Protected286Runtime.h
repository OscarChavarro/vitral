#ifndef __PROTECTED_286_RUNTIME__
#define __PROTECTED_286_RUNTIME__

#include "Protected286Types.h"

class Protected286Runtime {
public:
    static void initialize();
    static void reboot267System();
    static void acknowledgeMasterPic();
};

#endif
