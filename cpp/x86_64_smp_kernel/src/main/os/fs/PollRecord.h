#ifndef __POLL_RECORD__
#define __POLL_RECORD__
#include <stdint.h>

class PollRecord {
public:
    int32_t fd;
    int16_t events;
    int16_t revents;
};

#endif
