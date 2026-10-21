#include <stdio.h>

#include "java/lang/String.h"
#include "vsdk/toolkit/common/VSDK.h"
const double VSDK::EPSILON = 1e-6;

String VSDK::formatDouble(double a)
{
    char buf[64];
    snprintf(buf, sizeof(buf), "%.2f", a);
    return String(buf);
}

String VSDK::formatDouble(double a, int digits)
{
    if ( digits < 0 ) {
        digits = 0;
    }
    char buf[64];
    snprintf(buf, sizeof(buf), "%.*f", digits, a);
    return String(buf);
}

