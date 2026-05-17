#include "vsdk/toolkit/common/VSDK.h"

#include <iomanip>
#include <sstream>

const double VSDK::EPSILON = 1e-6;

std::string VSDK::formatDouble(double a)
{
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(2) << a;
    return oss.str();
}

std::string VSDK::formatDouble(double a, int digits)
{
    if ( digits < 0 ) {
        digits = 0;
    }
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(digits) << a;
    return oss.str();
}

