#ifndef __VITRAL_LIBC_SYS_TIME__
#define __VITRAL_LIBC_SYS_TIME__
#include <time.h>
struct timeval {
    time_t tv_sec;
    suseconds_t tv_usec;
};
struct timezone {
    int tz_minuteswest;
    int tz_dsttime;
};
int
gettimeofday(timeval *out, timezone *zone);

#endif
