#ifndef __DATE_TIME__
#define __DATE_TIME__
#include <stdint.h>

class DateTime {
public:
    int32_t year;
    uint32_t month;
    uint32_t day;
    uint32_t hour;
    uint32_t minute;
    uint32_t second;
    uint32_t weekday;
};

#endif
