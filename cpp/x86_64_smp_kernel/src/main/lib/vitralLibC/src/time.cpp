#include <time.h>
#include <sys/time.h>
#include <errno.h>
#include "kernel/Clock.h"
#include "lib/vitralKernel/Ports.h"
#include "syscalls/Syscalls.h"

static uint8_t
readRtc(uint8_t index) {
    Ports::outb(0x70, index);
    return Ports::inb(0x71);
}

static int64_t
daysFromCivil(int64_t year, unsigned month, unsigned day) {
    year -= month <= 2;
    int64_t era = (year >= 0 ? year : year - 399) / 400;
    unsigned yearOfEra = (unsigned)(year - era * 400);
    unsigned dayOfYear = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
    unsigned dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
    return era * 146097 + (int64_t)dayOfEra - 719468;
}

static int64_t
rtcSeconds() {
    while (readRtc(0x0A) & 0x80) {
    }
    uint8_t second = readRtc(0);
    uint8_t minute = readRtc(2);
    uint8_t hour = readRtc(4);
    uint8_t day = readRtc(7);
    uint8_t month = readRtc(8);
    uint8_t year = readRtc(9);
    uint8_t mode = readRtc(0x0B);
    if (!(mode & 4)) {
        auto decode = [](uint8_t v) { return (uint8_t)((v & 15) + (v >> 4) * 10); };
        second = decode(second);
        minute = decode(minute);
        hour = (uint8_t)(decode(hour & 0x7f) | (hour & 0x80));
        day = decode(day);
        month = decode(month);
        year = decode(year);
    }
    if (!(mode & 2) && (hour & 0x80)) {
        hour = (uint8_t)(((hour & 0x7f) + 12) % 24);
    }
    int64_t days = daysFromCivil(2000 + year, month, day);
    return days * 86400 + hour * 3600 + minute * 60 + second;
}

static int64_t bootWallSeconds = 0;
static bool bootWallKnown = false;

static int64_t
wallNanoseconds() {
    if (!bootWallKnown) {
        bootWallSeconds = rtcSeconds() - (int64_t)(Clock::nanoseconds() / 1000000000ull);
        bootWallKnown = true;
    }
    return bootWallSeconds * 1000000000ll + (int64_t)Clock::nanoseconds();
}

time_t
time(time_t *out) {
    time_t now = (time_t)(wallNanoseconds() / 1000000000ll);
    if (out) {
        *out = now;
    }
    return now;
}

clock_t
clock() { return (clock_t)(Clock::nanoseconds() / 1000); }
double
difftime(time_t end, time_t start) { return (double)(end - start); }

int
clock_gettime(clockid_t id, timespec *out) {
    int64_t ns = id == CLOCK_REALTIME ? wallNanoseconds() : (int64_t)Clock::nanoseconds();
    out->tv_sec = ns / 1000000000ll;
    out->tv_nsec = ns % 1000000000ll;
    return 0;
}

int
clock_getres(clockid_t, timespec *out) {
    out->tv_sec = 0;
    out->tv_nsec = 1;
    return 0;
}

int
gettimeofday(timeval *out, timezone *) {
    int64_t ns = wallNanoseconds();
    out->tv_sec = ns / 1000000000ll;
    out->tv_usec = (ns % 1000000000ll) / 1000;
    return 0;
}

int
nanosleep(const timespec *request, timespec *remaining) {
    Syscalls::TimeSpec ts = {request->tv_sec, request->tv_nsec};
    Syscalls::call(Syscalls::SYS_NANOSLEEP, (uint64_t)(uintptr_t)&ts);
    if (remaining) {
        remaining->tv_sec = remaining->tv_nsec = 0;
    }
    return 0;
}

tm *
gmtime_r(const time_t *value, tm *out) {
    int64_t seconds = *value;
    int64_t days = seconds / 86400;
    int64_t rest = seconds % 86400;
    if (rest < 0) {
        rest += 86400;
        days--;
    }
    out->tm_hour = (int)(rest / 3600);
    out->tm_min = (int)(rest % 3600 / 60);
    out->tm_sec = (int)(rest % 60);
    out->tm_wday = (int)((days + 4) % 7);
    if (out->tm_wday < 0) {
        out->tm_wday += 7;
    }
    int64_t shifted = days + 719468;
    int64_t era = (shifted >= 0 ? shifted : shifted - 146096) / 146097;
    unsigned dayOfEra = (unsigned)(shifted - era * 146097);
    unsigned yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365;
    int64_t year = (int64_t)yearOfEra + era * 400;
    unsigned dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100);
    unsigned mp = (5 * dayOfYear + 2) / 153;
    unsigned day = dayOfYear - (153 * mp + 2) / 5 + 1;
    unsigned month = mp < 10 ? mp + 3 : mp - 9;
    year += month <= 2;
    out->tm_year = (int)(year - 1900);
    out->tm_mon = (int)month - 1;
    out->tm_mday = (int)day;
    out->tm_yday = (int)(days - daysFromCivil(year, 1, 1));
    out->tm_isdst = 0;
    out->tm_gmtoff = 0;
    out->tm_zone = "UTC";
    return out;
}

static tm brokenDown;

tm *
gmtime(const time_t *value) { return gmtime_r(value, &brokenDown); }
tm *
localtime_r(const time_t *value, tm *out) { return gmtime_r(value, out); }
tm *
localtime(const time_t *value) { return gmtime_r(value, &brokenDown); }

time_t
mktime(tm *fields) {
    int64_t days = daysFromCivil(1900 + fields->tm_year + fields->tm_mon / 12, (unsigned)(fields->tm_mon % 12) + 1,
                                 (unsigned)fields->tm_mday);
    return (time_t)(days * 86400 + fields->tm_hour * 3600 + fields->tm_min * 60 + fields->tm_sec);
}

size_t
strftime(char *buffer, size_t size, const char *format, const tm *fields) {
    size_t n = 0;
    auto put = [&](char c) {
        if (n + 1 < size) {
            buffer[n] = c;
        }
        n++;
    };
    auto putNumber = [&](int value, int width) {
        char digits[12];
        int count = 0;
        do {
            digits[count++] = (char)('0' + value % 10);
            value /= 10;
        } while (value || count < width);
        while (count) {
            put(digits[--count]);
        }
    };
    for (; *format; format++) {
        if (*format != '%') {
            put(*format);
            continue;
        }
        switch (*++format) {
        case 'Y': putNumber(fields->tm_year + 1900, 4); break;
        case 'm': putNumber(fields->tm_mon + 1, 2); break;
        case 'd': putNumber(fields->tm_mday, 2); break;
        case 'H': putNumber(fields->tm_hour, 2); break;
        case 'M': putNumber(fields->tm_min, 2); break;
        case 'S': putNumber(fields->tm_sec, 2); break;
        case '%': put('%'); break;
        default: break;
        }
    }
    if (size) {
        buffer[n < size ? n : size - 1] = 0;
    }
    return n < size ? n : 0;
}

char *
asctime(const tm *) { return (char *)""; }
char *
ctime(const time_t *) { return (char *)""; }
