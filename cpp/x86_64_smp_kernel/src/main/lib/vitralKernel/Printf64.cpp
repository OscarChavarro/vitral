#include "lib/vitralKernel/Printf.h"
#include "lib/vitralKernel/PrintfImpl.h"
#include "lib/vitralKernel/Out64.h"

int
printf64(const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    int n = PrintfImpl::format<Out64>(fmt, ap);
    va_end(ap);
    return n;
}
