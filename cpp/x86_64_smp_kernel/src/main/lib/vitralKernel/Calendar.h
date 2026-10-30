#ifndef __CALENDAR__
#define __CALENDAR__
#include <stdint.h>
#include "lib/vitralKernel/DateTime.h"

class Calendar {
public:
    static bool isLeapYear(int32_t year);
    static unsigned daysInMonth(int32_t year, unsigned month);
    static bool isValid(const DateTime &date);
    static int64_t toUnix(const DateTime &date);
    static void fromUnix(int64_t seconds, DateTime &out);
    static const char *weekdayName(unsigned weekday);
    static const char *monthName(unsigned month);
};

#endif
