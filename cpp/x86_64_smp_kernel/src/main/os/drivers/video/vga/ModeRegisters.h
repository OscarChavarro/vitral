#ifndef __MODE_REGISTERS__
#define __MODE_REGISTERS__
#include <stdint.h>

class ModeRegisters {
public:
    uint8_t misc;
    uint8_t seq[5];
    uint8_t crtc[25];
    uint8_t gc[9];
    uint8_t ac[21];
};

#endif
