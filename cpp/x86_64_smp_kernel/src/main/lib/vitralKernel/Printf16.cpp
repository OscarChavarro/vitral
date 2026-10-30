#include "lib/vitralKernel/Printf.h"
#include "lib/vitralKernel/PrintfImpl.h"
#include "lib/vitralKernel/Out16.h"

int
printf16(const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    int n = PrintfImpl::format<Out16>(fmt, ap);
    va_end(ap);
    return n;
}
