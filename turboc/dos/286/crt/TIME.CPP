/* Poll the AT CMOS RTC directly. No interrupt handler or DOS service. */
#include <sys/timeb.h>
#include <errno.h>
#include "../Protected286Ports.h"
static unsigned char rtc(unsigned char index)
{
    unsigned char previous = Protected286Ports::inByte(0x70);
    Protected286Ports::outByte(0x70, (previous & 0x80) | index);
    unsigned char value = Protected286Ports::inByte(0x71);
    Protected286Ports::outByte(0x70, previous);
    return value;
}
static int decode(unsigned char value, int binary)
{
    if (binary) return value;
    if ((value & 15) > 9 || (value >> 4) > 9) return -1;
    return (value >> 4) * 10 + (value & 15);
}
static int leap(int year) { return !(year % 4) && (year % 100 || !(year % 400)); }
static int readTime(struct timeb* result)
{
    static const unsigned char months[] = {31,28,31,30,31,30,31,31,30,31,30,31};
    unsigned attempt;
    if (!result) { errno = EINVAL; return -1; }
    result->time = 0; result->millitm = 0; result->timezone = 0; result->dstflag = 0;
    for (attempt = 0; attempt < 8; ++attempt) {
        unsigned wait;
        for (wait = 0; wait < 4096 && (rtc(10) & 0x80); ++wait) {}
        if (wait == 4096) break;
        unsigned char mode = rtc(11), second = rtc(0), minute = rtc(2), hour = rtc(4);
        unsigned char day = rtc(7), month = rtc(8), year = rtc(9), century = rtc(0x32);
        if ((rtc(10) & 0x80) || rtc(0) != second || rtc(11) != mode) continue;
        int binary = mode & 4;
        int s = decode(second, binary), m = decode(minute, binary);
        int h = decode(hour & 0x7F, binary), d = decode(day, binary);
        int mo = decode(month, binary), y = decode(year, binary), c = decode(century, binary);
        if (!(mode & 2)) {
            if (h < 1 || h > 12) break;
            h = h % 12 + ((hour & 0x80) ? 12 : 0);
        }
        if (y < 0 || y > 99) break;
        y += (c >= 19 && c <= 21) ? c * 100 : (y >= 80 ? 1900 : 2000);
        if (s < 0 || s > 59 || m < 0 || m > 59 || h < 0 || h > 23 ||
            y < 1970 || mo < 1 || mo > 12 || d < 1 ||
            d > months[mo - 1] + (mo == 2 && leap(y))) break;
        unsigned long days = 0;
        for (int i = 1970; i < y; ++i) days += leap(i) ? 366 : 365;
        for (int j = 1; j < mo; ++j) days += months[j - 1] + (j == 2 && leap(y));
        days += d - 1;
        if (days > 24855UL) { errno = ERANGE; return -1; }
        unsigned long seconds = days * 86400UL + h * 3600UL + m * 60UL + s;
        if (seconds > 0x7FFFFFFFUL) { errno = ERANGE; return -1; }
        result->time = (long)seconds;
        return 0;
    }
    errno = EIO;
    return -1;
}
#ifdef __TURBOC__
extern "C" void ftime(struct timeb* result) { readTime(result); }
#else
extern "C" int ftime(struct timeb* result) { return readTime(result); }
#endif
