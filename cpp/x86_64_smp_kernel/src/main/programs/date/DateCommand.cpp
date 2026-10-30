#include "programs/date/DateCommand.h"
#include "lib/vitralKernel/Calendar.h"
#include "lib/vitralKernel/Printf.h"
#include "syscalls/Syscalls.h"
#include "lib/vitralKernel/DateTime.h"
#include "kernel/ArgList.h"

int
DateCommand::main(const ArgList &) {
    Syscalls::TimeSpec now;
    if (Syscalls::clockGetTime(Syscalls::CLOCK_WALL, &now) < 0) {
        printf64("date: cannot read the clock\n");
        return 1;
    }
    DateTime date;
    Calendar::fromUnix(now.seconds, date);
    printf64("%s %s %2u %02u:%02u:%02u UTC %d\n", Calendar::weekdayName(date.weekday), Calendar::monthName(date.month),
             date.day, date.hour, date.minute, date.second, date.year);
    return 0;
}
