#ifndef __VITRAL_LIBC_TIME__
#define __VITRAL_LIBC_TIME__
#include <stddef.h>
#include <sys/types.h>
#define CLOCKS_PER_SEC 1000000
#define CLOCK_REALTIME 0
#define CLOCK_MONOTONIC 1
#define CLOCK_PROCESS_CPUTIME_ID 2
typedef long clock_t;
typedef int clockid_t;
struct timespec {
    time_t tv_sec;
    long tv_nsec;
};
struct tm {
    int tm_sec;
    int tm_min;
    int tm_hour;
    int tm_mday;
    int tm_mon;
    int tm_year;
    int tm_wday;
    int tm_yday;
    int tm_isdst;
    long tm_gmtoff;
    const char *tm_zone;
};
time_t
time(time_t *out);
clock_t
clock();
double
difftime(time_t end, time_t start);
time_t
mktime(tm *fields);
tm *
gmtime(const time_t *value);
tm *
localtime(const time_t *value);
tm *
gmtime_r(const time_t *value, tm *out);
tm *
localtime_r(const time_t *value, tm *out);
size_t
strftime(char *buffer, size_t size, const char *format, const tm *fields);
char *
asctime(const tm *fields);
char *
ctime(const time_t *value);
int
clock_gettime(clockid_t clock, timespec *out);
int
clock_getres(clockid_t clock, timespec *out);
int
nanosleep(const timespec *request, timespec *remaining);

#endif
