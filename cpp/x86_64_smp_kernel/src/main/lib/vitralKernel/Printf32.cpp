#include "lib/vitralKernel/Printf.h"
#include "lib/vitralKernel/PrintfImpl.h"
#include "lib/vitralKernel/Out32.h"

int
printf32(const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    int n = PrintfImpl::format<Out32>(fmt, ap);
    va_end(ap);
    return n;
}
