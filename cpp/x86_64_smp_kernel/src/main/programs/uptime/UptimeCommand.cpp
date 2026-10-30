#include "programs/uptime/UptimeCommand.h"
#include "lib/vitralKernel/Calendar.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Printf.h"
#include "syscalls/Syscalls.h"
#include "lib/vitralKernel/DateTime.h"
#include "lib/vitralKernel/String.h"
#include "kernel/ArgList.h"

int
UptimeCommand::main(const ArgList &args) {
    int64_t started = 0;
    uint64_t seconds = Syscalls::uptime(&started);
    DateTime boot;
    Calendar::fromUnix(started, boot);
    if (args.count() == 1 && String::equals(args.at(0), "-s")) {
        printf64("%04d-%02u-%02u %02u:%02u:%02u\n", boot.year, boot.month, boot.day, boot.hour, boot.minute, boot.second);
        return 0;
    }
    if (args.count() != 0) {
        printf64("usage: uptime [-s]\n");
        return 1;
    }
    Syscalls::TimeSpec now;
    Syscalls::clockGetTime(Syscalls::CLOCK_WALL, &now);
    DateTime date;
    Calendar::fromUnix(now.seconds, date);
    uint64_t days = seconds / 86400;
    uint64_t hours = seconds / 3600 % 24;
    uint64_t minutes = seconds / 60 % 60;
    printf64(" %02u:%02u:%02u up ", date.hour, date.minute, date.second);
    if (days) {
        printf64("%u day%s, ", (uint32_t)days, days == 1 ? "" : "s");
    }
    printf64("%u:%02u:%02u, started %04d-%02u-%02u %02u:%02u:%02u UTC\n", (uint32_t)hours, (uint32_t)minutes,
             (uint32_t)(seconds % 60), boot.year, boot.month, boot.day, boot.hour, boot.minute, boot.second);
    return 0;
}
