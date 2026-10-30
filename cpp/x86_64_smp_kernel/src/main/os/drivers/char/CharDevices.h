#ifndef __CHAR_DEVICES__
#define __CHAR_DEVICES__
#include "drivers/char/CharDevice.h"

class CharDevices {
public:
    static constexpr unsigned MAX = 8;

    static bool add(CharDevice *device);
    static unsigned count();
    static CharDevice *at(unsigned index);

private:
    static CharDevice *table[MAX];
    static unsigned total;
};

#endif
