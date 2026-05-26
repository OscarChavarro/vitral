#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

#include <iomanip>
#include "java/lang/String.h"
#include <sstream>
#include "java/lang/String.h"

const double VSDK::EPSILON = 1e-6;

java::String VSDK::formatDouble(double a)
{
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(2) << a;
    return oss.str();
}

java::String VSDK::formatDouble(double a, int digits)
{
    if ( digits < 0 ) {
        digits = 0;
    }
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(digits) << a;
    return oss.str();
}

