#include "drivers/rtc/Rtc.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Ports.h"
#include "lib/vitralKernel/SpinLock.h"
#include "lib/vitralKernel/LockGuard.h"
#include "lib/vitralKernel/DateTime.h"

static SpinLock rtcLock;

uint8_t
Rtc::readRegister(uint8_t index) {
    Ports::outb(PORT_INDEX, index);
    return Ports::inb(PORT_DATA);
}

void
Rtc::readRaw(Raw &out) {
    LockGuard guard(rtcLock);
    Raw previous;
    Memory::set(&previous, 0xFF, sizeof(previous));
    for (int attempt = 0; attempt < 5; attempt++) {
        for (unsigned spin = 0; spin < 1000000 && (readRegister(REG_A) & 0x80); spin++) {}
        out.second = readRegister(REG_SECONDS);
        out.minute = readRegister(REG_MINUTES);
        out.hour = readRegister(REG_HOURS);
        out.day = readRegister(REG_DAY);
        out.month = readRegister(REG_MONTH);
        out.year = readRegister(REG_YEAR);
        out.century = readRegister(REG_CENTURY);
        if (Memory::compare(&out, &previous, sizeof(out)) == 0) {
            break;
        }
        previous = out;
    }
}

bool
Rtc::read(DateTime &out) {
    Raw raw;
    readRaw(raw);
    uint8_t statusB;
    {
        LockGuard guard(rtcLock);
        statusB = readRegister(REG_B);
    }
    bool binary = statusB & 0x04;
    bool hours24 = statusB & 0x02;
    bool pm = !hours24 && (raw.hour & 0x80);
    raw.hour &= 0x7F;
    auto decode = [binary](uint8_t v) -> uint32_t { return binary ? v : (uint32_t)((v >> 4) * 10 + (v & 15)); };

    uint32_t hour = decode(raw.hour);
    if (!hours24) {
        hour = (hour % 12) + (pm ? 12 : 0);
    }
    uint32_t century = decode(raw.century);
    uint32_t year = decode(raw.year);
    out.year = (int32_t)((century >= 19 && century <= 30 ? century * 100 : (year >= 70 ? 1900 : 2000)) + year);
    out.month = decode(raw.month);
    out.day = decode(raw.day);
    out.hour = hour;
    out.minute = decode(raw.minute);
    out.second = decode(raw.second);
    if (!Calendar::isValid(out)) {
        return false;
    }
    out.weekday = 0;
    DateTime derived;
    Calendar::fromUnix(Calendar::toUnix(out), derived);
    out.weekday = derived.weekday;
    return true;
}
