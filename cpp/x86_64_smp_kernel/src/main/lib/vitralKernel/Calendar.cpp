#include "lib/vitralKernel/Calendar.h"
#include "lib/vitralKernel/DateTime.h"

bool
Calendar::isLeapYear(int32_t year) { return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0; }

unsigned
Calendar::daysInMonth(int32_t year, unsigned month) {
    static const uint8_t DAYS[12] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    if (month < 1 || month > 12) {
        return 0;
    }
    return month == 2 && isLeapYear(year) ? 29 : DAYS[month - 1];
}

bool
Calendar::isValid(const DateTime &d) {
    return d.month >= 1 && d.month <= 12 && d.day >= 1 && d.day <= daysInMonth(d.year, d.month) && d.hour < 24 &&
           d.minute < 60 && d.second < 60;
}

int64_t
Calendar::toUnix(const DateTime &d) {
    int64_t y = d.year - (d.month <= 2 ? 1 : 0);
    int64_t era = (y >= 0 ? y : y - 399) / 400;
    int64_t yearOfEra = y - era * 400;
    int64_t dayOfYear = (153 * (d.month + (d.month > 2 ? -3 : 9)) + 2) / 5 + d.day - 1;
    int64_t dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
    int64_t days = era * 146097 + dayOfEra - 719468;
    return days * 86400 + d.hour * 3600 + d.minute * 60 + d.second;
}

void
Calendar::fromUnix(int64_t seconds, DateTime &out) {
    int64_t days = seconds / 86400;
    int64_t rest = seconds % 86400;
    if (rest < 0) { rest += 86400; days--; }
    out.hour = (uint32_t)(rest / 3600);
    out.minute = (uint32_t)(rest / 60 % 60);
    out.second = (uint32_t)(rest % 60);
    int64_t w = (days + 4) % 7;
    out.weekday = (uint32_t)(w < 0 ? w + 7 : w);

    days += 719468;
    int64_t era = (days >= 0 ? days : days - 146096) / 146097;
    int64_t dayOfEra = days - era * 146097;
    int64_t yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365;
    int64_t dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100);
    int64_t mp = (5 * dayOfYear + 2) / 153;
    out.day = (uint32_t)(dayOfYear - (153 * mp + 2) / 5 + 1);
    out.month = (uint32_t)(mp < 10 ? mp + 3 : mp - 9);
    out.year = (int32_t)(yearOfEra + era * 400 + (out.month <= 2 ? 1 : 0));
}

const char *
Calendar::weekdayName(unsigned weekday) {
    static const char *const NAMES[7] = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    return NAMES[weekday % 7];
}

const char *
Calendar::monthName(unsigned month) {
    static const char *const NAMES[12] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    return NAMES[(month + 11) % 12];
}
