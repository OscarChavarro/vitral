#ifndef __RTC__
#define __RTC__
#include "lib/vitralKernel/Calendar.h"
#include "lib/vitralKernel/DateTime.h"

class Rtc {
public:
    static bool read(DateTime &out);

private:
    static constexpr uint8_t PORT_INDEX = 0x70;
    static constexpr uint8_t PORT_DATA = 0x71;
    static constexpr uint8_t REG_SECONDS = 0x00;
    static constexpr uint8_t REG_MINUTES = 0x02;
    static constexpr uint8_t REG_HOURS = 0x04;
    static constexpr uint8_t REG_DAY = 0x07;
    static constexpr uint8_t REG_MONTH = 0x08;
    static constexpr uint8_t REG_YEAR = 0x09;
    static constexpr uint8_t REG_A = 0x0A;
    static constexpr uint8_t REG_B = 0x0B;
    static constexpr uint8_t REG_CENTURY = 0x32;

    class Raw {
    public:
        uint8_t second;
        uint8_t minute;
        uint8_t hour;
        uint8_t day;
        uint8_t month;
        uint8_t year;
        uint8_t century;
    };
    static uint8_t readRegister(uint8_t index);
    static void readRaw(Raw &out);
};

#endif
