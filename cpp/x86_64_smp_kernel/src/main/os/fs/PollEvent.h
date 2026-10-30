#ifndef __POLL_EVENT__
#define __POLL_EVENT__
#include <stdint.h>

class PollEvent {
public:
    static constexpr int16_t IN = 1;
    static constexpr int16_t OUT = 4;
    static constexpr int16_t ERR = 8;
    static constexpr int16_t HUP = 16;
    static constexpr int16_t NVAL = 32;
};

#endif
