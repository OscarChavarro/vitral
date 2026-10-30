#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include "kernel/Scheduler.h"

static int bootErrorNumber = 0;

int *
__errno() {
    if (!Scheduler::cpuCount()) {
        return &bootErrorNumber;
    }
    return Scheduler::current()->errorNumber();
}

void
__assert_fail(const char *expression, const char *file, int line) {
    fprintf(stderr, "assertion failed: %s (%s:%d)\n", expression, file, line);
    abort();
}
